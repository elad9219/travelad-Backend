package com.example.travelad.utils;

import com.example.travelad.beans.IataCodeEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IataCodeUtils {

    private static final Logger logger = LoggerFactory.getLogger(IataCodeUtils.class);
    // Map for FIELD2 (search IATA code)
    private static final Map<String, String> cityToIataMap = new HashMap<>();
    // Map for FIELD3 (final destination IATA code)
    private static final Map<String, String> cityToFinalDestinationMap = new HashMap<>();

    static {
        try {
            logger.info("Initializing IataCodeUtils...");
            ObjectMapper mapper = new ObjectMapper();
            List<IataCodeEntry> entries = mapper.readValue(
                    new ClassPathResource("iata_codes.json").getInputStream(),
                    new TypeReference<List<IataCodeEntry>>() {}
            );

            for (IataCodeEntry entry : entries) {
                if (entry.getIataCode() != null && entry.getField2() != null) {
                    // Use the full "IATA CODES" (converted to lower-case) as the key
                    cityToIataMap.put(entry.getIataCode().toLowerCase(), entry.getField2());
                }
                if (entry.getIataCode() != null) {
                    // For FIELD3, default to FIELD2 if FIELD3 is not provided
                    String finalDest = entry.getField3() != null ? entry.getField3() : entry.getField2();
                    cityToFinalDestinationMap.put(entry.getIataCode().toLowerCase(), finalDest);
                }
            }
            logger.info("IataCodeUtils initialized successfully.");
        } catch (IOException e) {
            logger.error("Error initializing IataCodeUtils: {}", e.getMessage(), e);
        }
    }

    public static String getIataCodeForCity(String city) {
        String lowerCity = city.toLowerCase().trim();
        // First, try an exact match.
        if (cityToIataMap.containsKey(lowerCity)) {
            return cityToIataMap.get(lowerCity);
        }
        // If not found, try a simple fuzzy match: if the key contains the search term or vice-versa.
        for (Map.Entry<String, String> entry : cityToIataMap.entrySet()) {
            String key = entry.getKey();
            if (key.contains(lowerCity) || lowerCity.contains(key)) {
                return entry.getValue();
            }
        }
        // If still not found, return null.
        return null;
    }

    public static String getFinalDestinationIataForCity(String city) {
        String lowerCity = city.toLowerCase().trim();
        // Try exact match first.
        if (cityToFinalDestinationMap.containsKey(lowerCity)) {
            return cityToFinalDestinationMap.get(lowerCity);
        }
        // Fuzzy match if exact match not found.
        for (Map.Entry<String, String> entry : cityToFinalDestinationMap.entrySet()) {
            String key = entry.getKey();
            if (key.contains(lowerCity) || lowerCity.contains(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static List<IataCodeEntry> getIataCodeEntries() {
        return cityToIataMap.entrySet().stream()
                .map(entry -> {
                    IataCodeEntry e = new IataCodeEntry();
                    e.setIataCode(entry.getKey());
                    e.setField2(entry.getValue());
                    return e;
                })
                .collect(Collectors.toList());
    }
}
