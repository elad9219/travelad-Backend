package com.example.travelad.service;

import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.Airline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class AirlineService {
    private static final Logger logger = LoggerFactory.getLogger(AirlineService.class);
    private Amadeus amadeus;

    @Value("${amadeus.api.key}")
    private String apiKey;

    @Value("${amadeus.api.secret}")
    private String apiSecret;

    private Map<String, String> iataToIcaoCache = new HashMap<>();

    @PostConstruct
    public void init() {
        this.amadeus = Amadeus.builder(apiKey, apiSecret).build();
    }

    public String getIcaoCode(String iataCode) {
        String key = iataCode.toUpperCase();
        if (iataToIcaoCache.containsKey(key)) {
            return iataToIcaoCache.get(key);
        }
        try {
            Airline[] airlines = amadeus.referenceData.airlines.get(Params.with("airlineCodes", key));
            if (airlines != null && airlines.length > 0) {
                String icao = airlines[0].getIcaoCode();
                iataToIcaoCache.put(key, icao);
                return icao;
            }
        } catch (ResponseException e) {
            logger.error("Error fetching airline info for {}: {}", key, e.getMessage());
        }
        return null;
    }

    public String getAirlineLogoUrl(String iataCode) {
        // Special handling for Alitalia: if the carrier code is "AZ", return the custom logo URL.
        if (iataCode != null && iataCode.equalsIgnoreCase("AZ")) {
            return "https://1000logos.net/wp-content/uploads/2019/12/Alitalia-Logo.png";
        }
        if (iataCode != null && iataCode.equalsIgnoreCase("5F")) {
            return "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR1ljeg2mAUrwpeBTNcUdWR4BQoORVXROAZfQ&s";
        }
        if (iataCode != null && iataCode.equalsIgnoreCase("H1")) {
            return "https://logos-world.net/wp-content/uploads/2023/01/Hahn-Air-Logo-500x281.png";
        }
        if (iataCode != null && iataCode.equalsIgnoreCase("H4")) {
            return "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTd2lzgYotP6HeSYoI2cLIHEjtJ0bVr3PxUGg&s";
        }
        String icaoCode = getIcaoCode(iataCode);
        if (icaoCode != null && !icaoCode.isEmpty()) {
            // Build the default URL using the GitHub raw URL and the ICAO code.
            return "https://raw.githubusercontent.com/sexym0nk3y/airline-logos/main/logos/" + icaoCode.toUpperCase() + ".png";
        }
        return null;
    }
}
