package com.example.travelad.service;

import com.example.travelad.beans.GeoapifyPlaceDto;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

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

    public List<GeoapifyPlaceDto> searchPlacesByCity(String cityName) {
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

            // Step 2: Find attractions using coordinates, with localization set to English
            String attractionsRequestUrl = String.format("%s?categories=tourism.sights&filter=circle:%s,%s,5000&apiKey=%s&lang=en",
                    placesUrl, lon, lat, apiKey);
            logger.info("Fetching attractions near city coordinates: {}, {}", lon, lat);

            String attractionsResponse = restTemplate.getForObject(attractionsRequestUrl, String.class);
            JSONObject attractionsJson = new JSONObject(attractionsResponse);

            // Step 3: Parse attractions and return the list of GeoapifyPlaceDto
            return parseAttractions(attractionsJson);
        } catch (Exception e) {
            logger.error("Error fetching attractions for city: {}: {}", cityName, e.getMessage());
            throw new RuntimeException("Error fetching attractions", e);
        }
    }

    private List<GeoapifyPlaceDto> parseAttractions(JSONObject attractionsJson) {
        List<GeoapifyPlaceDto> places = new ArrayList<>();
        JSONArray features = attractionsJson.getJSONArray("features");

        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            JSONObject properties = feature.getJSONObject("properties");
            JSONObject geometry = feature.getJSONObject("geometry");

            // Extract additional details
            String street = properties.optString("address_line2", "No address available");
            String postcode = properties.optString("postcode", "No postcode available");
            String phone = properties.optString("phone", "No phone number available");
            String website = properties.optString("website", "No website available");
            String opening_hours = properties.optString("opening_hours", "No opening hours available");



            GeoapifyPlaceDto place = new GeoapifyPlaceDto(
                    properties.optString("name", "Unknown Place"),
                    properties.optString("city", "Unknown City"),
                    properties.optString("country", "Unknown Country"),
                    properties.optString("description", "No description available")
            );

            // Add more details to the place
            place.setAddress(street);
            place.setPostcode(postcode);
            place.setPhone(phone);
            place.setWebsite(website);
            place.setOpening_hours(opening_hours);

            places.add(place);
        }

        return places;
    }
}
