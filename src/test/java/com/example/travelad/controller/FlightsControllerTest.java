package com.example.travelad.controller;

import com.example.travelad.service.FlightsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlightsController.class)
@Import(FlightsService.class)
class FlightsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private FlightsService service;

    private static String future() {
        return LocalDate.now().plusDays(10).toString();
    }

    private MockHttpServletRequestBuilder advanced(
            String origin,
            String destination,
            String departure,
            String returning,
            String adults) {
        return get("/flights/advancedFlightSearch")
                .param("origin", origin)
                .param("destination", destination)
                .param("departDate", departure)
                .param("returnDate", returning)
                .param("adults", adults);
    }

    @Test
    void regularSearchReturnsRoundTripDemoOffersWithTheExistingDefaults()
            throws Exception {
        mvc.perform(get("/flights").param("city", "Paris"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].outboundSegments[0].origin")
                                .value("TLV")
                )
                .andExpect(
                        jsonPath("$[0].outboundSegments[-1].destination")
                                .value(isOneOf("CDG", "ORY"))
                )
                .andExpect(
                        jsonPath("$[0].returnSegments[0].origin")
                                .value(isOneOf("CDG", "ORY"))
                )
                .andExpect(
                        jsonPath("$[0].returnSegments[-1].destination")
                                .value("TLV")
                )
                .andExpect(jsonPath("$[0].price", greaterThan(0.0)));
    }

    @Test
    void unknownCityReturnsAnEmptyJsonArray() throws Exception {
        mvc.perform(
                        get("/flights")
                                .param("city", "NoSuchCityPortfolioProbe")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void blankRegularCityIsBadRequestInsteadOfMatchingTheFirstIataEntry()
            throws Exception {
        mvc.perform(get("/flights").param("city", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oneWaySearchWorksWithoutAReturnParameter() throws Exception {
        mvc.perform(
                        get("/flights/advancedFlightSearch")
                                .param("origin", "Tel Aviv")
                                .param("destination", "Paris")
                                .param("departDate", future())
                                .param("adults", "2")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].segments[0].origin").value("TLV")
                )
                .andExpect(
                        jsonPath("$[0].segments[-1].destination")
                                .value(isOneOf("CDG", "ORY"))
                )
                .andExpect(jsonPath("$[0].returnSegments").isEmpty());
    }

    @Test
    void roundTripAllowsASameDayReturnAndNinePassengers() throws Exception {
        mvc.perform(
                        advanced(
                                "Tel Aviv",
                                "Paris",
                                future(),
                                future(),
                                "9"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].returnSegments[-1].destination")
                                .value("TLV")
                );
    }

    static Stream<Arguments> invalidSearches() {
        return Stream.of(
                Arguments.of(future(), "", "0"),
                Arguments.of(future(), "", "10"),
                Arguments.of(future(), "", "2.5"),
                Arguments.of(future(), "", "1e2"),
                Arguments.of(future(), "", ""),
                Arguments.of("invalid-date", "", "1"),
                Arguments.of("", "", "1"),
                Arguments.of("2027-02-30", "", "1"),
                Arguments.of(
                        LocalDate.now().minusDays(1).toString(),
                        "",
                        "1"
                ),
                Arguments.of(
                        future(),
                        LocalDate.now().plusDays(9).toString(),
                        "1"
                ),
                Arguments.of(future(), "bad-return", "1")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidSearches")
    void invalidDatesAndPassengerCountsAreBadRequestsThroughApplicationAdvice(
            String departure,
            String returning,
            String adults) throws Exception {
        mvc.perform(
                        advanced(
                                "Tel Aviv",
                                "Paris",
                                departure,
                                returning,
                                adults
                        )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void invalidAndIdenticalCitiesAreRejected() throws Exception {
        mvc.perform(advanced(" ", "Paris", future(), "", "1"))
                .andExpect(status().isBadRequest());

        mvc.perform(
                        advanced(
                                "NoSuchCityPortfolioProbe",
                                "Paris",
                                future(),
                                "",
                                "1"
                        )
                )
                .andExpect(status().isBadRequest());

        mvc.perform(advanced("Paris", "Paris", future(), "", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void directServiceCallsCannotBypassInputValidation() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.flights("TL", "PAR", future(), "1", "")
        );
        assertThrows(
                ResponseStatusException.class,
                () -> service.flights("TLV", "PAR", future(), "0", "")
        );
        assertThrows(
                ResponseStatusException.class,
                () -> service.flights("TLV", "PAR", "invalid-date", "1", "")
        );
    }
}