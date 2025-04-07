package com.example.travelad.service;

import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.exceptions.ResponseException;
import com.example.travelad.repositories.AirlineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AirlineService {
    private static final Logger logger = LoggerFactory.getLogger(AirlineService.class);
    private Amadeus amadeus;

    @Value("${amadeus.api.key}")
    private String apiKey;

    @Value("${amadeus.api.secret}")
    private String apiSecret;

    private Map<String, String> iataToIcaoCache = new HashMap<>();
    private final AirlineRepository airlineRepository;

    // Constructor עם הזרקת תלות
    public AirlineService(AirlineRepository airlineRepository) {
        this.airlineRepository = airlineRepository;
    }

    @PostConstruct
    public void init() {
        this.amadeus = Amadeus.builder(apiKey, apiSecret).build();
    }

    public String getIcaoCode(String iataCode) {
        String key = iataCode != null ? iataCode.toUpperCase() : null;
        if (key == null || key.isEmpty()) {
            logger.warn("Invalid IATA code provided: null or empty");
            return null;
        }
        if (iataToIcaoCache.containsKey(key)) {
            return iataToIcaoCache.get(key);
        }

        Optional<com.example.travelad.beans.Airline> cachedAirline = airlineRepository.findById(key);
        if (cachedAirline.isPresent()) {
            String icao = cachedAirline.get().getIcaoCode();
            iataToIcaoCache.put(key, icao);
            logger.info("Retrieved ICAO code from database for IATA code: {}", key);
            return icao;
        }

        try {
            com.amadeus.resources.Airline[] airlines = amadeus.referenceData.airlines.get(Params.with("airlineCodes", key));
            if (airlines != null && airlines.length > 0) {
                String icao = airlines[0].getIcaoCode();
                iataToIcaoCache.put(key, icao);
                com.example.travelad.beans.Airline airlineEntity = new com.example.travelad.beans.Airline();
                airlineEntity.setIataCode(key);
                airlineEntity.setIcaoCode(icao);
                airlineRepository.save(airlineEntity);
                logger.info("Fetched ICAO code {} for IATA code {} from Amadeus API and saved to database", icao, key);
                return icao;
            } else {
                logger.warn("No airline data found for IATA code: {}", key);
                return null;
            }
        } catch (ResponseException e) {
            logger.error("Error fetching airline info for {}: {}", key, e.getMessage());
            return null;
        }
    }

    public String getAirlineLogoUrl(String iataCode) {
        if (iataCode == null || iataCode.isEmpty()) {
            logger.warn("Invalid IATA code provided for logo URL: null or empty");
            return null;
        }
        if (iataCode.equalsIgnoreCase("AZ")) {
            return "https://1000logos.net/wp-content/uploads/2019/12/Alitalia-Logo.png";
        }
        if (iataCode.equalsIgnoreCase("5F")) {
            return "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR1ljeg2mAUrwpeBTNcUdWR4BQoORVXROAZfQ&s";
        }
        if (iataCode.equalsIgnoreCase("H1")) {
            return "https://logos-world.net/wp-content/uploads/2023/01/Hahn-Air-Logo-500x281.png";
        }
        if (iataCode.equalsIgnoreCase("Q1")) {
            return "https://1000logos.net/wp-content/uploads/2020/03/Qatar-Airways-Logo.png";
        }

        Optional<com.example.travelad.beans.Airline> cachedAirline = airlineRepository.findById(iataCode.toUpperCase());
        if (cachedAirline.isPresent() && cachedAirline.get().getLogoUrl() != null) {
            logger.info("Retrieved logo URL from database for IATA code: {}", iataCode);
            return cachedAirline.get().getLogoUrl();
        }

        String icaoCode = getIcaoCode(iataCode);
        if (icaoCode != null && !icaoCode.isEmpty()) {
            String logoUrl = "https://raw.githubusercontent.com/sexym0nk3y/airline-logos/main/logos/" + icaoCode.toUpperCase() + ".png";
            com.example.travelad.beans.Airline airlineEntity = cachedAirline.orElse(new com.example.travelad.beans.Airline());
            airlineEntity.setIataCode(iataCode.toUpperCase());
            airlineEntity.setIcaoCode(icaoCode);
            airlineEntity.setLogoUrl(logoUrl);
            airlineRepository.save(airlineEntity);
            logger.info("Saved logo URL {} for IATA code {} to database", logoUrl, iataCode);
            return logoUrl;
        }
        logger.warn("No logo URL available for IATA code: {}", iataCode);
        return null;
    }
}