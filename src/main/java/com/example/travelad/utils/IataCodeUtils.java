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
    private static final Map<String, String> cityToIataMap = new HashMap<>();

    static {
        try {
            logger.info("Initializing IataCodeUtils...");
            ObjectMapper mapper = new ObjectMapper();
            List<IataCodeEntry> entries = mapper.readValue(
                    new ClassPathResource("iata_codes.json").getInputStream(),
                    new TypeReference<List<IataCodeEntry>>() {}
            );

            for (IataCodeEntry entry : entries) {
                if (entry.getField2() != null && entry.getIataCode() != null) {
                    cityToIataMap.put(entry.getIataCode().toLowerCase(), entry.getField2());
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

    public static List<IataCodeEntry> getIataCodeEntries() {
        // If you need the raw list of entries
        return List.copyOf(cityToIataMap.entrySet().stream()
                .map(entry -> new IataCodeEntry() {{
                    setIataCode(entry.getKey());
                    setField2(entry.getValue());
                }})
                .collect(Collectors.toList()));
    }
}