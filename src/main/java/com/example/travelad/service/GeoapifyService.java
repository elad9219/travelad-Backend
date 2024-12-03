package com.example.travelad.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import kong.unirest.json.JSONObject;

import javax.annotation.PostConstruct;

@Service
public class GeoapifyService {

    private static final Logger logger = LoggerFactory.getLogger(GeoapifyService.class);

    private final RestTemplate restTemplate;

    @Value("${geoapify.api.key}")
    private String apiKey;

    private String geocodingUrl;
    private String placesUrl;

    public GeoapifyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        geocodingUrl = "https://api.geoapify.com/v1/geocode/search";
        placesUrl = "https://api.geoapify.com/v2/places";
    }

    public String findAttractionsByCity(String cityName) {
        try {
            // Step 1: Get city coordinates
            String geocodingRequestUrl = String.format("%s?text=%s&apiKey=%s", geocodingUrl, cityName, apiKey);
            logger.info("Fetching coordinates for city: {}", cityName);

            String geocodingResponse = restTemplate.getForObject(geocodingRequestUrl, String.class);
            JSONObject geocodingJson = new JSONObject(geocodingResponse);

            // Extract coordinates
            JSONObject cityLocation = geocodingJson.getJSONArray("features").getJSONObject(0).getJSONObject("geometry");
            String lon = cityLocation.getJSONArray("coordinates").get(0).toString();
            String lat = cityLocation.getJSONArray("coordinates").get(1).toString();

            // Step 2: Find attractions using coordinates
            String attractionsRequestUrl = String.format("%s?categories=tourism.sights&filter=circle:%s,%s,5000&apiKey=%s",
                    placesUrl, lon, lat, apiKey);
            logger.info("Fetching attractions near city coordinates: {}, {}", lon, lat);

            return restTemplate.getForObject(attractionsRequestUrl, String.class);
        } catch (Exception e) {
            logger.error("Error fetching attractions for city: {}: {}", cityName, e.getMessage());
            throw e;
        }
    }
}
