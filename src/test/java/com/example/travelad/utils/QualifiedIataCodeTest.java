package com.example.travelad.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QualifiedIataCodeTest {

    @Test
    void resolvesCitiesWithTheirCountryAndRegion() {
        assertEquals("PAR", IataCodeUtils.getIataCodeForCity(
                "Paris, Ile-de-France, France"
        ));

        assertEquals("PAR", IataCodeUtils.getIataCodeForCity(
                "Paris,France"
        ));

        assertEquals("LON", IataCodeUtils.getIataCodeForCity(
                "London, England, United Kingdom"
        ));

        assertEquals("NYC", IataCodeUtils.getIataCodeForCity(
                "New York City, New York, United States"
        ));

        assertEquals("BCN", IataCodeUtils.getIataCodeForCity(
                "Barcelona, Catalonia, Spain"
        ));

        assertEquals("BLA", IataCodeUtils.getIataCodeForCity(
                "Barcelona, Anzoátegui, Venezuela"
        ));

        assertEquals("YTO", IataCodeUtils.getIataCodeForCity(
                "Toronto, Canada"
        ));
    }

    @Test
    void neverFallsBackToAnotherCityWhenQualificationIsUnknown() {
        assertNull(IataCodeUtils.getIataCodeForCity(
                "Paris, Texas, United States"
        ));

        assertNull(IataCodeUtils.getIataCodeForCity(
                "London, France"
        ));

        assertNull(IataCodeUtils.getIataCodeForCity(
                "Barcelona"
        ));
    }
}