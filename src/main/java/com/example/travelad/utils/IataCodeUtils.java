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
                    // Use the value in "IATA CODES" (converted to lower-case) as the key
                    cityToIataMap.put(entry.getIataCode().toLowerCase(), entry.getField2());
                }
                if (entry.getIataCode() != null) {
                    // For FIELD3, if not provided, default to FIELD2
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
        return cityToIataMap.get(city.toLowerCase());
    }

    public static String getFinalDestinationIataForCity(String city) {
        return cityToFinalDestinationMap.get(city.toLowerCase());
    }

    public static java.util.List<IataCodeEntry> getIataCodeEntries() {
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
