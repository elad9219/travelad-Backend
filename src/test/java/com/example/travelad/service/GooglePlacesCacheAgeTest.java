package com.example.travelad.service;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.repositories.GooglePlacesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GooglePlacesCacheAgeTest {

    private final GooglePlacesRepository repository = mock(GooglePlacesRepository.class);
    private final RestTemplate http = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(http).build();
    private final GooglePlacesService service =
            new GooglePlacesService(http, repository, mock(GooglePhotoUrlResolver.class), "test-key");

    @Test
    void expiredCoordinatesAreRefreshedUsingTheExistingRow() {
        GooglePlaces expired = stored(LocalDateTime.now().minusDays(30));
        when(repository.findByCityIgnoreCase("paris")).thenReturn(List.of(expired));
        when(repository.findByPlaceId("paris-id")).thenReturn(Optional.of(expired));
        server.expect(requestTo(containsString("/textsearch/json"))).andRespond(withSuccess("""
                {"status":"OK","results":[{"place_id":"paris-id","name":"Paris",
                  "formatted_address":"Paris, France",
                  "geometry":{"location":{"lat":48.86,"lng":2.35}}}]}
                """, MediaType.APPLICATION_JSON));

        assertEquals(48.86, service.findPlaceByCity("Paris").getLatitude());
        assertEquals(48.86, service.findPlaceByCity("Paris").getLatitude());
        verify(repository).save(argThat(place -> Long.valueOf(7).equals(place.getId()) &&
                place.getCreatedAt().isAfter(expired.getCreatedAt())));
        server.verify();
    }

    @Test
    void undatedCoordinatesAreNotServedOnProviderFailure() {
        when(repository.findByCityIgnoreCase("paris")).thenReturn(List.of(stored(null)));
        server.expect(requestTo(containsString("/textsearch/json"))).andRespond(withServerError());

        assertTrue(service.findPlaceByCity("Paris").getPlaceId().startsWith("unknown_"));
        server.verify();
    }

    private GooglePlaces stored(LocalDateTime fetchedAt) {
        GooglePlaces place = new GooglePlaces();
        place.setId(7L);
        place.setPlaceId("paris-id");
        place.setName("Paris");
        place.setLatitude(48.85);
        place.setLongitude(2.35);
        place.setCreatedAt(fetchedAt);
        return place;
    }
}