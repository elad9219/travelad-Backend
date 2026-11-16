package com.example.travelad.service;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.config.AppConfig;
import com.example.travelad.repositories.GooglePlacesRepository;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GooglePlacesServiceTest {

    private static final String PLACE =
            "{\"status\":\"OK\",\"results\":[{\"place_id\":\"known-place\","
                    + "\"name\":\"Paris\",\"formatted_address\":\"Paris, France\","
                    + "\"geometry\":{\"location\":{\"lat\":48.85,\"lng\":2.35}}}]}";

    private final GooglePlacesRepository repository =
            mock(GooglePlacesRepository.class);

    private final RestTemplate http = new RestTemplate();

    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(http).build();

    private final GooglePlacesService service =
            new GooglePlacesService(http, repository, "test-key");

    private GooglePlaces stored() {
        GooglePlaces place = new GooglePlaces();
        place.setPlaceId("known-place");
        place.setName("Paris");
        place.setLatitude(48.85);
        place.setLongitude(2.35);
        place.setIcon(
                "https://old.example/photo?key=old-key&photo_reference=old-reference"
        );
        return place;
    }

    private void textSearch(String response) {
        server.expect(requestTo(
                        org.hamcrest.Matchers.containsString("/textsearch/json")
                ))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    private void photo(String reference) {
        server.expect(requestTo(
                        org.hamcrest.Matchers.containsString("/details/json")
                ))
                .andRespond(withSuccess(
                        "{\"status\":\"OK\",\"result\":{\"photos\":["
                                + "{\"photo_reference\":\"" + reference + "\"}]}}",
                        MediaType.APPLICATION_JSON
                ));
    }

    @Test
    void queryIsEncodedOnceIncludingUnicodeAndReservedCharacters() {
        String city = "東京 A+B%20&C";

        server.expect(request -> {
            String query = request.getURI().getRawQuery();
            String encoded = java.util.Arrays.stream(query.split("&"))
                    .filter(value -> value.startsWith("query="))
                    .findFirst()
                    .orElseThrow()
                    .substring(6);

            assertEquals(
                    city,
                    URLDecoder.decode(encoded, StandardCharsets.UTF_8)
            );
            assertTrue(query.contains("%2B"));
            assertTrue(query.contains("%2520"));
        }).andRespond(withSuccess(PLACE, MediaType.APPLICATION_JSON));

        assertEquals(
                "known-place",
                service.findPlaceByCity(city).getPlaceId()
        );

        server.verify();
    }

    @Test
    void databaseReadAndWriteFailuresDoNotDiscardProviderData() {
        when(repository.findByCityIgnoreCase("paris"))
                .thenThrow(new IllegalStateException());
        when(repository.save(any()))
                .thenThrow(new IllegalStateException());

        textSearch(PLACE);

        assertEquals(
                "known-place",
                service.findPlaceByCity("Paris").getPlaceId()
        );
        assertEquals(
                "known-place",
                service.findPlaceByCity(" PARIS ").getPlaceId()
        );

        verify(repository, times(1)).findByCityIgnoreCase("paris");
        server.verify();
    }

    @Test
    void providerFailureIsNotCachedAndCanRecoverOnTheNextRequest() {
        textSearch("{\"status\":\"OVER_QUERY_LIMIT\",\"results\":[]}");
        textSearch(PLACE);

        assertTrue(
                service.findPlaceByCity("Paris")
                        .getPlaceId()
                        .startsWith("unknown_")
        );
        assertEquals(
                "known-place",
                service.findPlaceByCity("Paris").getPlaceId()
        );

        verify(repository, times(1)).save(any());
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"status\":\"OK\",\"results\":[]}",
            "{\"status\":\"OK\",\"results\":[{\"place_id\":\"known\",\"name\":\"Paris\"}]}",
            "{\"status\":\"OK\",\"results\":[{\"place_id\":\"known\",\"name\":\"Paris\",\"geometry\":{\"location\":{\"lat\":95,\"lng\":2}}}]}",
            "{\"status\":\"OK\",\"results\":[{\"place_id\":\"\",\"name\":\"Paris\",\"geometry\":{\"location\":{\"lat\":48,\"lng\":2}}}]}"
    })
    void malformedProviderDataIsNotPersisted(String response) {
        textSearch(response);

        assertTrue(
                service.findPlaceByCity("Paris")
                        .getPlaceId()
                        .startsWith("unknown_")
        );

        verify(repository, never()).save(any());
        server.verify();
    }

    @Test
    void photoFailureKeepsValidDetailsAndDoesNotMutateTheStoredEntity() {
        GooglePlaces original = stored();
        when(repository.findByCityIgnoreCase("paris"))
                .thenReturn(List.of(original));

        server.expect(requestTo(
                        org.hamcrest.Matchers.containsString("/details/json")
                ))
                .andRespond(withServerError());

        GooglePlaces result = service.searchPlaceByCity("Paris");

        assertEquals("known-place", result.getPlaceId());
        assertEquals("", result.getIcon());
        assertTrue(original.getIcon().contains("old-reference"));

        server.verify();
    }

    @Test
    void freshPhotoReferencesAreNeitherPersistedNorReusedByMetadataConsumers() {
        textSearch(PLACE);
        photo("reference-one");
        photo("reference-two");

        assertTrue(
                service.searchPlaceByCity("Paris")
                        .getIcon()
                        .contains("reference-one")
        );
        assertEquals("", service.findPlaceByCity("Paris").getIcon());
        assertTrue(
                service.searchPlaceByCity("Paris")
                        .getIcon()
                        .contains("reference-two")
        );

        verify(repository).save(argThat(place -> "".equals(place.getIcon())));
        server.verify();
    }

    @Test
    void callersCannotMutateTheCachedEntity() {
        when(repository.findByCityIgnoreCase("paris"))
                .thenReturn(List.of(stored()));

        GooglePlaces first = service.findPlaceByCity("Paris");
        first.setName("Changed");
        first.setLatitude(null);

        assertEquals("Paris", service.findPlaceByCity("Paris").getName());
        assertEquals(48.85, service.findPlaceByCity("Paris").getLatitude());

        verify(repository, times(1)).findByCityIgnoreCase("paris");
    }

    @Test
    void simultaneousMetadataRequestsShareOneLookup() throws Exception {
        when(repository.findByCityIgnoreCase("paris"))
                .thenReturn(List.of(stored()));

        ExecutorService workers = Executors.newFixedThreadPool(8);

        try {
            List<Callable<GooglePlaces>> calls = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                calls.add(() -> service.findPlaceByCity("Paris"));
            }

            for (Future<GooglePlaces> result : workers.invokeAll(calls)) {
                assertEquals("known-place", result.get().getPlaceId());
            }

            verify(repository, times(1)).findByCityIgnoreCase("paris");
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void cacheIsBoundedAndEvictsOlderCities() {
        when(repository.findByCityIgnoreCase(anyString()))
                .thenAnswer(call -> List.of(stored()));

        for (int i = 0; i <= 100; i++) {
            service.findPlaceByCity("City " + i);
        }

        service.findPlaceByCity("City 0");

        verify(repository, times(2)).findByCityIgnoreCase("city 0");
    }

    @Test
    void metadataCacheUsesLocaleIndependentCityKeys() {
        Locale previous = Locale.getDefault();

        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            when(repository.findByCityIgnoreCase("istanbul"))
                    .thenReturn(List.of(stored()));

            service.findPlaceByCity("ISTANBUL");

            verify(repository).findByCityIgnoreCase("istanbul");
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void hotelSearchUsesMetadataWithoutFetchingCityPhotos() {
        when(repository.findByCityIgnoreCase("paris+100%"))
                .thenReturn(List.of(stored()));

        HotelsService hotels = new HotelsService(service);

        List<com.example.travelad.dto.HotelDto> results =
                hotels.searchHotelsByCityName(
                        "Paris+100%",
                        "2026-11-20",
                        "2026-11-22",
                        1
                );

        assertEquals(25, results.size());
        assertTrue(
                results.stream().allMatch(
                        hotel -> hotel.getName().contains("Paris+100%")
                )
        );

        verify(repository).findByCityIgnoreCase("paris+100%");
        server.verify();
    }

    @Test
    void outboundHttpClientDoesNotReplayADisconnectedRequest() throws Exception {
        AtomicInteger requests = new AtomicInteger();

        HttpServer endpoint = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        endpoint.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.close();
        });
        endpoint.start();

        AppConfig config = new AppConfig();

        try (CloseableHttpClient client = config.outboundHttpClient()) {
            RestTemplate realHttp = config.restTemplate(client);
            URI uri = URI.create(
                    "http://127.0.0.1:" + endpoint.getAddress().getPort() + "/"
            );

            assertThrows(
                    RestClientException.class,
                    () -> realHttp.getForObject(uri, String.class)
            );
            assertEquals(1, requests.get());
        } finally {
            endpoint.stop(0);
        }
    }
}