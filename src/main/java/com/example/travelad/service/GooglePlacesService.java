package com.example.travelad.service;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.repositories.GooglePlacesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GooglePlacesService {

    private static final Logger logger = LoggerFactory.getLogger(GooglePlacesService.class);
    private final RestTemplate restTemplate;
    private final GooglePlacesRepository googlePlacesRepository;
    private final String googleApiKey;
    private final Map<String, GooglePlaces> localCache = new HashMap<>(); // Local in-memory cache

    @Autowired
    public GooglePlacesService(RestTemplate restTemplate, GooglePlacesRepository googlePlacesRepository,
                               @Value("${google.places.api.key}") String googleApiKey) {
        this.restTemplate = restTemplate;
        this.googlePlacesRepository = googlePlacesRepository;
        this.googleApiKey = googleApiKey;
    }

    public GooglePlaces searchPlaceByCity(String city) {
        try {
            // Normalize and encode city name
            String normalizedCity = city.trim().toLowerCase();
            String encodedCity = UriComponentsBuilder.fromUriString(normalizedCity)
                    .build()
                    .toUriString()
                    .replace(" ", "+");
            logger.debug("Searching for city: {}, normalized: {}, encoded: {}", city, normalizedCity, encodedCity);

            // Check local cache first
            if (localCache.containsKey(normalizedCity)) {
                GooglePlaces place = localCache.get(normalizedCity);
                String photoUrl = getPhotoUrl(place.getPlaceId());
                place.setIcon(photoUrl != null ? photoUrl : "https://via.placeholder.com/150");
                logger.info("Returning locally cached GooglePlaces for city: {}", normalizedCity);
                return place;
            }

            // Check database cache
            List<GooglePlaces> cachedPlaces = googlePlacesRepository.findByCityIgnoreCase(normalizedCity);
            if (!cachedPlaces.isEmpty()) {
                GooglePlaces place = cachedPlaces.get(0);
                String photoUrl = getPhotoUrl(place.getPlaceId());
                place.setIcon(photoUrl != null ? photoUrl : "https://via.placeholder.com/150");
                localCache.put(normalizedCity, place); // Update local cache
                logger.info("Returning database cached GooglePlaces for city: {}", normalizedCity);
                return place;
            }

            // Fetch from Google Places API
            String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/place/textsearch/json")
                    .queryParam("query", encodedCity)
                    .queryParam("key", googleApiKey)
                    .toUriString();
            logger.debug("Calling Google Places API with URL: {}", url);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                logger.warn("Google Places API returned null response for city: {}", normalizedCity);
                return createEnhancedFallbackPlace(normalizedCity);
            }

            String status = (String) response.get("status");
            if (!"OK".equals(status)) {
                logger.warn("Google Places API returned non-OK status: {} for city: {}", status, normalizedCity);
                return createEnhancedFallbackPlace(normalizedCity);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null || results.isEmpty()) {
                logger.warn("Google Places API returned empty or null results for city: {}", normalizedCity);
                return createEnhancedFallbackPlace(normalizedCity);
            }

            Map<String, Object> result = results.get(0);
            String placeId = (String) result.getOrDefault("place_id", "unknown_" + normalizedCity);
            GooglePlaces place;

            // Check if place_id already exists in database
            Optional<GooglePlaces> existingPlaceOpt = googlePlacesRepository.findByPlaceId(placeId);
            if (existingPlaceOpt.isPresent()) {
                place = existingPlaceOpt.get();
                logger.info("Found existing GooglePlaces for place_id: {}", placeId);
            } else {
                place = new GooglePlaces();
                place.setPlaceId(placeId);
            }

            place.setCity(normalizedCity);
            place.setName((String) result.getOrDefault("name", normalizedCity));
            place.setAddress((String) result.getOrDefault("formatted_address", normalizedCity));
            @SuppressWarnings("unchecked")
            Map<String, Object> geometry = (Map<String, Object>) result.getOrDefault("geometry", new HashMap<>());
            @SuppressWarnings("unchecked")
            Map<String, Double> location = (Map<String, Double>) geometry.getOrDefault("location", new HashMap<>());
            place.setLatitude(location.getOrDefault("lat", 0.0));
            place.setLongitude(location.getOrDefault("lng", 0.0));
            place.setAttractionCount(0); // Placeholder, can be enhanced later
            place.setComplete(true);
            place.setCountry(extractCountry((String) result.get("formatted_address")));

            // Fetch photo directly
            String photoUrl = getPhotoUrl(place.getPlaceId());
            place.setIcon(photoUrl != null ? photoUrl : "https://via.placeholder.com/150");

            // Save or update in database
            try {
                googlePlacesRepository.save(place);
                logger.info("Saved/Updated GooglePlaces for city {} in database", normalizedCity);
            } catch (Exception e) {
                logger.error("Error saving GooglePlaces for city {}: {}", normalizedCity, e.getMessage(), e);
                return createEnhancedFallbackPlace(normalizedCity);
            }

            // Save to local cache
            localCache.put(normalizedCity, place);
            logger.info("Returning GooglePlaces for city: {}", normalizedCity);
            return place;

        } catch (Exception e) {
            logger.error("Error fetching place for city {}: {}", city, e.getMessage(), e);
            return createEnhancedFallbackPlace(city);
        }
    }

    private String getPhotoUrl(String placeId) {
        try {
            String detailsUrl = "https://maps.googleapis.com/maps/api/place/details/json" +
                    "?place_id=" + placeId +
                    "&fields=photos" +
                    "&key=" + googleApiKey;
            logger.debug("Fetching photo with URL: {}", detailsUrl);
            Map<String, Object> response = restTemplate.getForObject(detailsUrl, Map.class);
            if (response == null) {
                logger.warn("Google Places Details API returned null response for placeId: {}", placeId);
                return null;
            }

            if (response.containsKey("status") && !"OK".equals(response.get("status"))) {
                logger.warn("Google Places Details API returned non-OK status: {} for placeId: {}", response.get("status"), placeId);
                return null;
            }

            if (response.containsKey("result")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                if (result.containsKey("photos")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> photos = (List<Map<String, Object>>) result.get("photos");
                    if (!photos.isEmpty()) {
                        String photoReference = (String) photos.get(0).get("photo_reference");
                        String photoUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                                "?maxwidth=1600" +
                                "&photo_reference=" + photoReference +
                                "&key=" + googleApiKey;
                        logger.debug("Generated photo URL: {}", photoUrl);
                        return photoUrl;
                    } else {
                        logger.warn("No photos found for placeId: {}", placeId);
                    }
                } else {
                    logger.warn("Response missing 'photos' key for placeId: {}", placeId);
                }
            } else {
                logger.warn("Response missing 'result' key for placeId: {}", placeId);
            }
            return null;
        } catch (Exception e) {
            logger.error("Error fetching photo for place {}: {}", placeId, e.getMessage(), e);
            return null;
        }
    }

    private String extractCountry(String address) {
        if (address == null) return "";
        String[] parts = address.split(",");
        if (parts.length > 0) {
            return parts[parts.length - 1].trim();
        }
        return "";
    }

    private GooglePlaces createEnhancedFallbackPlace(String city) {
        GooglePlaces place = new GooglePlaces();
        place.setCity(city);
        place.setName(city);
        place.setAddress(city + ", Unknown"); // Improved fallback address
        place.setIcon("https://via.placeholder.com/150");
        place.setLatitude(0.0);
        place.setLongitude(0.0);
        place.setPlaceId("unknown_" + city);
        place.setAttractionCount(0); // Placeholder
        place.setComplete(false);
        place.setCountry("");
        logger.info("Created enhanced fallback place for city: {}", city);
        return place;
    }
}