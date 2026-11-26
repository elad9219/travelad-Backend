package com.example.travelad.utils;

import com.example.travelad.beans.IataCodeEntry;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FlightAirportMetadataTest {

    @Test
    void everyPhysicalAirportHasExactlyOneDisplayName() throws Exception {
        Set<String> codes = new HashSet<>();

        try (var reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/mock_flight_airports.tsv"),
                StandardCharsets.UTF_8))) {

            for (String line; (line = reader.readLine()) != null;) {
                if (line.startsWith("A\t")) {
                    codes.add(line.split("\t")[1]);
                }
            }
        }

        assertTrue(codes.size() > 3000);

        Map<String, String> names = new HashMap<>();

        for (IataCodeEntry entry : IataCodeUtils.getIataCodeEntries()) {
            if (codes.contains(entry.getAirportCode())) {
                assertNotNull(entry.getFullName());
                assertFalse(entry.getFullName().isBlank());
                assertNull(names.put(
                        entry.getAirportCode(),
                        entry.getFullName()
                ));
            }
        }

        assertEquals(codes, names.keySet());

        assertEquals("Ben Gurion International Airport", names.get("TLV"));
        assertEquals("London Heathrow Airport", names.get("LHR"));
        assertEquals("Charles de Gaulle International Airport", names.get("CDG"));
        assertEquals("César Manrique-Lanzarote Airport", names.get("ACE"));

        for (String code : codes) {
            assertEquals(code, IataCodeUtils.getIataCodeForCity(code));
        }
    }

    @Test
    void resolvesPreviouslyMissingAirportCodesAndFullNames() {
        assertEquals("ACE", IataCodeUtils.getIataCodeForCity("ACE"));
        assertEquals("ACE", IataCodeUtils.getIataCodeForCity("ace"));

        assertEquals("ACE", IataCodeUtils.getIataCodeForCity(
                "Cesar Manrique-Lanzarote Airport"
        ));

        assertEquals("LHR", IataCodeUtils.getIataCodeForCity(
                "London Heathrow Airport"
        ));

        assertEquals("CDG", IataCodeUtils.getIataCodeForCity(
                "Charles de Gaulle International Airport"
        ));

        assertNull(IataCodeUtils.getIataCodeForCity("Heathrow Air"));
        assertNull(IataCodeUtils.getIataCodeForCity("ZZZ"));
    }

    @Test
    void cityAndMetropolitanResolutionRemainsSeparate() {
        assertEquals("LON", IataCodeUtils.getIataCodeForCity("London"));
        assertEquals("LON", IataCodeUtils.getIataCodeForCity("LON"));
        assertEquals("PAR", IataCodeUtils.getIataCodeForCity("Paris"));
        assertEquals("NYC", IataCodeUtils.getIataCodeForCity("New York"));
        assertEquals("TLV", IataCodeUtils.getIataCodeForCity("Tel Aviv"));

        assertEquals("BCN", IataCodeUtils.getIataCodeForCity(
                "Barcelona, Spain"
        ));

        assertNull(IataCodeUtils.getIataCodeForCity("Barcelona"));
        assertNull(IataCodeUtils.getIataCodeForCity("London, France"));
    }
}