package com.example.travelad.controller;

import com.example.travelad.service.GooglePlacesService;
import com.example.travelad.service.HotelsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HotelsController.class)
@Import(HotelsService.class)
class HotelsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private HotelsService service;

    @MockitoBean
    private GooglePlacesService places;

    private static String future() {
        return LocalDate.now().plusDays(10).toString();
    }

    @Test
    void regularSearchKeepsOptionalDefaultsAndExistingDemoOffers()
            throws Exception {

        mvc.perform(get("/hotels/by-city-name")
                        .param("cityName", "Paris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(25))
                .andExpect(jsonPath("$[0].price", greaterThan(0.0)));

        verify(places).findPlaceByCity("Paris");
    }

    @Test
    void advancedSearchAcceptsNineGuestsAndPreservesLiteralCityCharacters()
            throws Exception {

        mvc.perform(get("/hotels/by-city-name")
                        .param("cityName", "  Paris+100%  ")
                        .param("checkInDate", future())
                        .param(
                                "checkOutDate",
                                LocalDate.now().plusDays(12).toString())
                        .param("adults", "9"))
                .andExpect(status().isOk());

        verify(places).findPlaceByCity("Paris+100%");
    }

    @Test
    void missingCheckoutDefaultsToTheNightAfterTheRequestedCheckIn()
            throws Exception {

        mvc.perform(get("/hotels/by-city-name")
                        .param("cityName", "Paris")
                        .param("checkInDate", future()))
                .andExpect(status().isOk());
    }

    static Stream<Arguments> invalidSearches() {
        String start = future();
        String end = LocalDate.now().plusDays(11).toString();

        return Stream.of(
                Arguments.of(" ", start, end, "1"),
                Arguments.of("Paris\nprobe", start, end, "1"),
                Arguments.of("x".repeat(121), start, end, "1"),
                Arguments.of("Paris", "", end, "1"),
                Arguments.of("Paris", "invalid-date", end, "1"),
                Arguments.of("Paris", "2027-02-30", end, "1"),
                Arguments.of(
                        "Paris",
                        LocalDate.now().minusDays(1).toString(),
                        end,
                        "1"),
                Arguments.of("Paris", start, "", "1"),
                Arguments.of("Paris", start, "invalid-date", "1"),
                Arguments.of("Paris", start, start, "1"),
                Arguments.of(
                        "Paris",
                        start,
                        LocalDate.now().plusDays(9).toString(),
                        "1"),
                Arguments.of("Paris", start, end, "0"),
                Arguments.of("Paris", start, end, "-1"),
                Arguments.of("Paris", start, end, "10"),
                Arguments.of("Paris", start, end, "2.5"),
                Arguments.of("Paris", start, end, "1e2")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidSearches")
    void invalidInputsAreRejectedBeforeAnyProviderCall(
            String city,
            String checkIn,
            String checkOut,
            String adults) throws Exception {

        mvc.perform(get("/hotels/by-city-name")
                        .param("cityName", city)
                        .param("checkInDate", checkIn)
                        .param("checkOutDate", checkOut)
                        .param("adults", adults))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verifyNoInteractions(places);
    }

    @Test
    void missingCityUsesTheApplicationBadRequestResponse()
            throws Exception {

        mvc.perform(get("/hotels/by-city-name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verifyNoInteractions(places);
    }

    @Test
    void directServiceCallsCannotBypassValidation() {
        ResponseStatusException invalidGuests = assertThrows(
                ResponseStatusException.class,
                () -> service.searchHotelsByCityName(
                        "Paris", future(), null, 10));

        ResponseStatusException invalidDates = assertThrows(
                ResponseStatusException.class,
                () -> service.searchHotelsByCityName(
                        "Paris", future(), future(), 1));

        assertEquals(
                HttpStatus.BAD_REQUEST,
                invalidGuests.getStatusCode());

        assertEquals(
                HttpStatus.BAD_REQUEST,
                invalidDates.getStatusCode());

        verifyNoInteractions(places);
    }

    @Test
    void offersEndpointRetainsItsExistingEmptyDemoContract()
            throws Exception {

        mvc.perform(get("/hotels/offers")
                        .param("hotelIds", "MOCK_1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verifyNoInteractions(places);
    }
}