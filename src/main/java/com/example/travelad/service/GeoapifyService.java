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
            if (geocodingResponse == null) {
                throw new RuntimeException("No response from Geoapify Geocoding API");
            }

            JSONObject geocodingJson = new JSONObject(geocodingResponse);
            JSONArray features = geocodingJson.optJSONArray("features");
            if (features == null || features.isEmpty()) {
                throw new RuntimeException("No features found for city: " + cityName);
            }

            JSONObject cityLocation = features.getJSONObject(0).getJSONObject("geometry");
            String lon = cityLocation.getJSONArray("coordinates").get(0).toString();
            String lat = cityLocation.getJSONArray("coordinates").get(1).toString();

            // Step 2: Fetch attractions
            String attractionsRequestUrl = String.format("%s?categories=tourism.sights&filter=circle:%s,%s,5000&apiKey=%s&lang=en",
                    placesUrl, lon, lat, apiKey);
            logger.info("Fetching attractions near city coordinates: {}, {}", lon, lat);

            String attractionsResponse = restTemplate.getForObject(attractionsRequestUrl, String.class);
            if (attractionsResponse == null) {
                throw new RuntimeException("No response from Geoapify Places API");
            }

            JSONObject attractionsJson = new JSONObject(attractionsResponse);
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

            // Extract phone from contact if available
            String phone = properties.optJSONObject("contact") != null
                    ? properties.getJSONObject("contact").optString("phone", "No phone number available")
                    : "No phone number available";

            // Get the English name if available, otherwise fall back to local name
            String name = properties.optJSONObject("name_international") != null
                    ? properties.getJSONObject("name_international").optString("en", properties.optString("name", "Unknown Name"))
                    : properties.optString("name", "Unknown Name");

            // Extract other optional properties
            String street = properties.optString("address_line2", "No address available");
            String website = properties.optString("website", "No website available");
            String opening_hours = properties.optString("opening_hours", "No opening hours available");

            // Create the place DTO with the name being either English or local
            GeoapifyPlaceDto place = new GeoapifyPlaceDto(
                    name,  // Use the correct name here
                    properties.optString("city", "Unknown City"),
                    properties.optString("country", "Unknown Country"),
                    properties.optString("description", "No description available")
            );

            // Set additional details to the place object
            place.setAddress(street);
            place.setPhone(phone);
            place.setWebsite(website);
            place.setOpening_hours(opening_hours);

            // Add place to the list
            places.add(place);
        }

        return places;
    }
}
