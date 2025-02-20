package com.example.travelad.utils;

import com.example.travelad.beans.IataCodeEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

public class IataCodeUtils {

    private static final Logger logger = LoggerFactory.getLogger(IataCodeUtils.class);
    // Load the original list of entries from the JSON file.
    private static List<IataCodeEntry> originalEntries;

    static {
        try {
            logger.info("Initializing IataCodeUtils...");
            ObjectMapper mapper = new ObjectMapper();
            originalEntries = mapper.readValue(
                    new ClassPathResource("iata_codes.json").getInputStream(),
                    new TypeReference<List<IataCodeEntry>>() {}
            );
            // For each entry, if AIRPORT CODE is null or empty, set it to the value from "FULL NAME".
            for (IataCodeEntry entry : originalEntries) {
                if (entry.getAirportCode() == null || entry.getAirportCode().trim().isEmpty()) {
                    // Fallback: use the value from "IATA CODES" (i.e. getIataCode()).
                    entry.setAirportCode((entry.getFullName() != null && !entry.getFullName().trim().isEmpty())
                            ? entry.getFullName().trim()
                            : entry.getCityCode());
                }
            }
            logger.info("IataCodeUtils initialized successfully with {} entries.", originalEntries.size());
        } catch (IOException e) {
            logger.error("Error initializing IataCodeUtils: {}", e.getMessage(), e);
        }
    }

    // Returns the original list of IATA code entries.
    public static List<IataCodeEntry> getIataCodeEntries() {
        return originalEntries;
    }

    // Lookup method: Given a city (as full name, e.g., "Tel Aviv"), return the corresponding IATA code (FIELD2).
    public static String getIataCodeForCity(String city) {
        if (originalEntries == null) return null;
        String search = city.toLowerCase().trim();
        for (IataCodeEntry entry : originalEntries) {
            if (entry.getFullName() != null && entry.getFullName().toLowerCase().contains(search)) {
                return entry.getCityCode().toUpperCase().trim();
            }
        }
        return null;
    }

    // Lookup method: Given a city (full name), return the final destination IATA code.
    // If FIELD3 is available (non-null and non-empty), it is returned; otherwise, FIELD2 is returned.
    public static String getFinalDestinationIataForCity(String city) {
        if (originalEntries == null) return null;
        String search = city.toLowerCase().trim();
        for (IataCodeEntry entry : originalEntries) {
            if (entry.getFullName() != null && entry.getFullName().toLowerCase().contains(search)) {
                if (entry.getAirportCode() != null && !entry.getAirportCode().trim().isEmpty()) {
                    return entry.getAirportCode().toUpperCase().trim();
                } else {
                    return entry.getCityCode().toUpperCase().trim();
                }
            }
        }
        return null;
    }
}
