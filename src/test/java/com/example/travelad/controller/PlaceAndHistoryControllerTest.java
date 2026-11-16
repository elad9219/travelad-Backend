package com.example.travelad.controller;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.service.CityCacheService;
import com.example.travelad.service.GooglePlacesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlaceAndHistoryControllerTest {

    private final GooglePlacesService places = mock(GooglePlacesService.class);
    private final CityCacheService history = mock(CityCacheService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(
                new GooglePlacesController(places, history),
                new CityCacheController(history)
        ).build();
    }

    private GooglePlaces place() {
        GooglePlaces place = new GooglePlaces();
        place.setPlaceId("known-place");
        place.setName("Tokyo");
        return place;
    }

    @Test
    void queryParametersAreDecodedExactlyOnce() throws Exception {
        when(places.searchPlaceByCity(anyString())).thenReturn(place());

        mvc.perform(get(URI.create(
                        "/api/places/search?city=A%2BB%2520C&userId=user-1"
                )))
                .andExpect(status().isOk());

        verify(places).searchPlaceByCity("A+B%20C");
        verify(history).addCity("user-1", "A+B%20C");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "Tokyo\nInjected"})
    void invalidCityDoesNotCallTheProvider(String city) throws Exception {
        mvc.perform(get("/api/places/search").param("city", city))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(places, history);
    }

    @Test
    void oversizedCityAndInvalidHistoryIdentifierAreRejected() throws Exception {
        mvc.perform(get("/api/places/search").param("city", "a".repeat(121)))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/places/search")
                        .param("city", "Tokyo")
                        .param("userId", "bad:id"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(places, history);
    }

    @Test
    void providerFallbackIsNotASuccessAndDoesNotWriteHistory() throws Exception {
        GooglePlaces fallback = place();
        fallback.setPlaceId("unknown_Tokyo");
        when(places.searchPlaceByCity("Tokyo")).thenReturn(fallback);

        mvc.perform(get("/api/places/search")
                        .param("city", "Tokyo")
                        .param("userId", "user-1"))
                .andExpect(status().isBadGateway());

        verifyNoInteractions(history);
    }

    @Test
    void providerFailureAndNullResponseAreNotEmptySuccesses() throws Exception {
        when(places.searchPlaceByCity("Tokyo"))
                .thenThrow(new IllegalStateException("private-provider-detail"));

        mvc.perform(get("/api/places/search").param("city", "Tokyo"))
                .andExpect(status().isBadGateway())
                .andExpect(content().string(""));

        when(places.searchPlaceByCity("Paris")).thenReturn(null);

        mvc.perform(get("/api/places/search").param("city", "Paris"))
                .andExpect(status().isBadGateway());

        verifyNoInteractions(history);
    }

    @Test
    void historyOutageDoesNotDiscardAValidPlace() throws Exception {
        when(places.searchPlaceByCity("Tokyo")).thenReturn(place());
        when(history.addCity("user-1", "Tokyo"))
                .thenThrow(new IllegalStateException());

        mvc.perform(get("/api/places/search")
                        .param("city", " Tokyo ")
                        .param("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeId").value("known-place"));

        verify(history, times(1)).addCity("user-1", "Tokyo");
    }

    @Test
    void anonymousSearchDoesNotCreateAnUnreachableHistoryKey() throws Exception {
        when(places.searchPlaceByCity("東京")).thenReturn(place());

        mvc.perform(get("/api/places/search").param("city", "東京"))
                .andExpect(status().isOk());

        verifyNoInteractions(history);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cache", "/api/places/cache"})
    void bothHistoryRoutesPreserveLiteralCharactersAndSupportMutations(
            String base) throws Exception {
        when(history.getCities("user-1")).thenReturn(List.of("A+B%20C"));
        when(history.addCity("user-1", "A+B%20C")).thenReturn("added");
        when(history.removeCity("user-1", "A+B%20C")).thenReturn("removed");
        when(history.clearCities("user-1")).thenReturn("cleared");

        mvc.perform(get(base + "/cities").param("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("A+B%20C"));

        mvc.perform(post(base + "/cities")
                        .param("userId", "user-1")
                        .param("city", " A+B%20C "))
                .andExpect(status().isOk())
                .andExpect(content().string("added"));

        mvc.perform(delete(base + "/cities")
                        .param("userId", "user-1")
                        .param("city", "A+B%20C"))
                .andExpect(status().isOk())
                .andExpect(content().string("removed"));

        mvc.perform(delete(base + "/cities/clear").param("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("cleared"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cache", "/api/places/cache"})
    void historyRoutesValidateDirectRequests(String base) throws Exception {
        mvc.perform(get(base + "/cities").param("userId", "bad:id"))
                .andExpect(status().isBadRequest());

        mvc.perform(post(base + "/cities")
                        .param("userId", "user-1")
                        .param("city", " "))
                .andExpect(status().isBadRequest());

        mvc.perform(delete(base + "/cities")
                        .param("userId", "user-1")
                        .param("city", "\n"))
                .andExpect(status().isBadRequest());

        mvc.perform(delete(base + "/cities/clear").param("userId", ""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(history);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cache", "/api/places/cache"})
    void historyOutageIsDistinctFromEmptyHistory(String base) throws Exception {
        when(history.getCities("user-1"))
                .thenThrow(new IllegalStateException("private-redis-detail"));

        mvc.perform(get(base + "/cities").param("userId", "user-1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("[]"));

        when(history.getCities("portfolio_probe_123")).thenReturn(List.of());

        mvc.perform(get(base + "/cities").param("userId", "portfolio_probe_123"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}