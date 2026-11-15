package com.example.travelad.service;

import com.example.travelad.beans.Attraction;
import com.example.travelad.beans.AttractionCacheStatus;
import com.example.travelad.repositories.AttractionCacheStatusRepository;
import com.example.travelad.repositories.AttractionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AttractionsServiceTest {
    private final AttractionRepository repository = mock(AttractionRepository.class);
    private final AttractionCacheStatusRepository statuses = mock(AttractionCacheStatusRepository.class);
    private final AttractionImageService images = mock(AttractionImageService.class);
    private final RestTemplate http = new RestTemplate();
    private final AttractionsService service = new AttractionsService(repository, statuses, images, "test-key", http);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(http).build();

    private Attraction cached(String name, int version, int order) {
        Attraction a = new Attraction();
        a.setId((long) order + 1); a.setName(name); a.setCity("paris");
        a.setLatitude(48.85); a.setLongitude(2.35);
        a.setSelectionVersion(version); a.setDisplayOrder(order);
        return a;
    }

    private void geocode() {
        server.expect(requestTo(containsString("/geocode/search"))).andRespond(withSuccess(
                "{\"features\":[{\"properties\":{\"place_id\":\"city-id\",\"lat\":48.85,\"lon\":2.35,\"country_code\":\"fr\"}}]}", MediaType.APPLICATION_JSON));
    }

    private String feature(String name, String category) {
        return "{\"properties\":{\"name\":\"" + name + "\",\"lat\":48.85,\"lon\":2.35,\"categories\":[\"" + category + "\"]}}";
    }

    private void places(String category, String limit, String features) {
        server.expect(requestTo(containsString("/v2/places")))
                .andExpect(queryParam("categories", category)).andExpect(queryParam("limit", limit))
                .andExpect(request -> assertFalse(request.getURI().getRawQuery().contains("bias=")))
                .andRespond(withSuccess("{\"features\":[" + features + "]}", MediaType.APPLICATION_JSON));
    }

    @Test void preservesHyphenatedCitiesAndRejectsBlankInput() {
        assertEquals("saint-denis", AttractionsService.normalizeCity(" Saint-Denis "));
        assertThrows(IllegalArgumentException.class, () -> service.searchPlacesByCity(" "));
        verifyNoInteractions(repository, statuses, images);
    }

    @Test void cacheReturnsPersistedOrderRatherThanDatabaseOrder() {
        AttractionCacheStatus status = new AttractionCacheStatus("paris", true);
        status.setVersion(3); status.setRefreshedAt(System.currentTimeMillis());
        Attraction first = cached("Tower", 3, 0);
        Attraction second = cached("Museum", 3, 1);
        when(statuses.findById("paris")).thenReturn(Optional.of(status));
        when(repository.findByCityIgnoreCase("paris")).thenReturn(List.of(second, first));
        assertEquals(List.of(first, second), service.searchPlacesByCity("Paris"));
        verifyNoInteractions(images);
        server.verify();
    }

    @Test void refreshesVersionTwoAndPersistsRankingAndExistingId() {
        Attraction old = cached("Tower", 2, 0);
        when(statuses.findById("paris")).thenReturn(Optional.of(new AttractionCacheStatus("paris", true)));
        when(repository.findByCityIgnoreCase("paris")).thenReturn(List.of(old));
        when(images.enrich(anyList(), anyMap(), anyString())).thenAnswer(call -> {
            AttractionImageService.Result result = new AttractionImageService.Result();
            List<Attraction> values = call.getArgument(0);
            values.forEach(a -> result.sitelinks.put(a, a.getName().equals("Tower") ? 100 : 1));
            return result;
        });
        geocode();
        places("tourism.sights,tourism.attraction", "200", feature("Plaque", "tourism.sights.memorial")
                + "," + feature("Tower", "tourism.sights.tower"));
        places("entertainment.museum", "100", feature("Museum", "entertainment.museum"));
        places("leisure.park", "60", "");
        List<Attraction> result = service.searchPlacesByCity("Paris");
        assertEquals("Tower", result.get(0).getName());
        assertEquals(old.getId(), result.get(0).getId());
        for (int i = 0; i < result.size(); i++) {
            assertEquals(3, result.get(i).getSelectionVersion());
            assertEquals(i, result.get(i).getDisplayOrder());
        }
        verify(statuses).save(argThat(s -> s.isComplete() && s.getVersion() == 3));
        server.verify();
    }

    @Test void failedRefreshKeepsPreviousSelectionAndHonorsCooldown() {
        Attraction old = cached("Tower", 2, 0);
        AttractionCacheStatus status = new AttractionCacheStatus("paris", true);
        when(statuses.findById("paris")).thenReturn(Optional.of(status));
        when(repository.findByCityIgnoreCase("paris")).thenReturn(List.of(old));
        server.expect(requestTo(containsString("/geocode/search"))).andRespond(withServerError());
        assertEquals(List.of(old), service.searchPlacesByCity("Paris"));
        assertEquals(List.of(old), service.searchPlacesByCity("Paris"));
        assertEquals(2, old.getSelectionVersion());
        verify(repository, never()).saveAll(anyList());
        server.verify();
    }

    @Test void partialDiscoveryDoesNotReplaceCompletePreviousSelection() {
        Attraction old = cached("Tower", 2, 0);
        when(repository.findByCityIgnoreCase("paris")).thenReturn(List.of(old));
        geocode();
        places("tourism.sights,tourism.attraction", "200", feature("Plaque", "tourism.sights.memorial"));
        server.expect(requestTo(containsString("/v2/places"))).andRespond(withServerError());
        places("leisure.park", "60", "");
        assertEquals(List.of(old), service.searchPlacesByCity("Paris"));
        verify(repository, never()).saveAll(anyList());
        verifyNoInteractions(images);
        server.verify();
    }

    @Test void imageOutageDoesNotReplaceExistingPicturesWithAnEmptySelection() {
        Attraction old = cached("Tower", 2, 0);
        old.setImageUrl("https://thumb.wikimedia.org/wikipedia/commons/a/a1/Tower.jpg");
        when(repository.findByCityIgnoreCase("paris")).thenReturn(List.of(old));
        AttractionImageService.Result failed = new AttractionImageService.Result(); failed.complete = false;
        when(images.enrich(anyList(), anyMap(), anyString())).thenReturn(failed);
        geocode();
        places("tourism.sights,tourism.attraction", "200", feature("Tower", "tourism.sights.tower"));
        places("entertainment.museum", "100", "");
        places("leisure.park", "60", "");
        assertEquals(List.of(old), service.searchPlacesByCity("Paris"));
        verify(repository, never()).saveAll(anyList());
        server.verify();
    }

    @Test void discoveryFailureIsNotAnEmptySuccess() {
        server.expect(requestTo(containsString("/geocode/search"))).andRespond(withServerError());
        assertThrows(IllegalStateException.class, () -> service.searchPlacesByCity("Paris"));
        verify(statuses).save(argThat(s -> !s.isComplete() && s.getVersion() == 3));
        verify(repository, never()).saveAll(anyList());
        server.verify();
    }

    @Test void categoryHierarchyDoesNotPromotePlaquesAsMajorSights() throws Exception {
        ObjectMapper json = new ObjectMapper();
        double plaque = AttractionsService.score(json.readTree("{\"categories\":[\"tourism\",\"tourism.sights\",\"tourism.sights.memorial\"]}"), 1);
        double tower = AttractionsService.score(json.readTree("{\"categories\":[\"tourism\",\"tourism.sights\",\"tourism.sights.tower\"]}"), 100);
        assertTrue(tower > plaque);
        assertEquals(0, AttractionsService.score(json.readTree("{\"categories\":[\"office\"]}"), 100));
    }
}