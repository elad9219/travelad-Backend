package com.example.travelad.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IataCodeUtilsTest {

    @Test
    void resolvesExactNamesAndExistingAliases() {
        assertEquals("NYC", IataCodeUtils.getIataCodeForCity("New York"));
        assertEquals("PAR", IataCodeUtils.getIataCodeForCity("  PARIS "));
        assertEquals("LON", IataCodeUtils.getIataCodeForCity("London"));
        assertEquals("TLV", IataCodeUtils.getIataCodeForCity("Tel Aviv"));
        assertEquals("SAO", IataCodeUtils.getIataCodeForCity("São   Paulo"));
    }

    @Test
    void acceptsKnownCityAndAirportCodes() {
        assertEquals("NYC", IataCodeUtils.getIataCodeForCity("nyc"));
        assertEquals("JFK", IataCodeUtils.getIataCodeForCity("JFK"));
        assertEquals("LHR", IataCodeUtils.getFinalDestinationIataForCity("London, Heathrow"));
        assertNull(IataCodeUtils.getIataCodeForCity("QQQ"));
    }

    @Test
    void refusesPartialNamesAndUnresolvedAmbiguity() {
        assertNull(IataCodeUtils.getIataCodeForCity("York"));
        assertNull(IataCodeUtils.getIataCodeForCity("Paris, Texas"));
        assertNull(IataCodeUtils.getIataCodeForCity("Barcelona"));
        assertEquals("BCN", IataCodeUtils.getIataCodeForCity("BCN"));
        assertEquals("BLA", IataCodeUtils.getIataCodeForCity("BLA"));
    }
}