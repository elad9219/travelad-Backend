package com.example.travelad.service;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.repositories.GooglePlacesRepository;
import kong.unirest.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class GooglePlacesService {

    private final GooglePlacesRepository googlePlacesRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.places.api.key}")
    private String apiKey;

    public GooglePlacesService(GooglePlacesRepository googlePlacesRepository) {
        this.googlePlacesRepository = googlePlacesRepository;
    }

    public GooglePlaces searchPlaceByCity(String cityName) {
        String normalizedCity = cityName.toLowerCase();
        // Check if city data is already cached in the database
        List<GooglePlaces> cachedPlaces = null;
        try {
            cachedPlaces = googlePlacesRepository.findByCityIgnoreCase(normalizedCity);
            if (cachedPlaces != null && !cachedPlaces.isEmpty()) {
                return cachedPlaces.get(0); // Return the first cached place
            }
        } catch (DataAccessException e) {
            System.err.println("Database error while fetching places for city " + normalizedCity + ": " + e.getMessage() + ". Falling back to API.");
        }

        // Fetch from Google Places API if not cached or database fails
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" + normalizedCity + "&key=" + apiKey;
        String response = restTemplate.getForObject(url, String.class);

        // Parse API response
        JSONObject responseJson = new JSONObject(response);

        // Get the first result and save it
        if (responseJson.has("results") && responseJson.getJSONArray("results").length() > 0) {
            JSONObject firstResult = responseJson.getJSONArray("results").getJSONObject(0);
            return savePlaceFromApiResponse(firstResult, normalizedCity);
        }

        return null; // No result found
    }

    private GooglePlaces savePlaceFromApiResponse(JSONObject result, String cityName) {
        String placeId = result.getString("place_id");

        // Check if the placeId already exists in the database
        Optional<GooglePlaces> existingPlace = Optional.empty();
        try {
            existingPlace = googlePlacesRepository.findByPlaceId(placeId);
        } catch (DataAccessException e) {
            System.err.println("Database error while checking existing place " + placeId + ": " + e.getMessage() + ". Proceeding without database check.");
        }

        GooglePlaces place;
        if (existingPlace.isPresent()) {
            place = existingPlace.get();
        } else {
            place = new GooglePlaces();
            place.setPlaceId(placeId);
        }

        // Update fields
        place.setName(result.optString("name"));
        place.setAddress(result.optString("formatted_address"));
        place.setLatitude(result.getJSONObject("geometry").getJSONObject("location").getDouble("lat"));
        place.setLongitude(result.getJSONObject("geometry").getJSONObject("location").getDouble("lng"));
        place.setCity(cityName);

        // Update icon with photo URL if available
        if (result.has("photos")) {
            JSONObject photo = result.getJSONArray("photos").getJSONObject(0);
            String photoReference = photo.getString("photo_reference");
            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=3400&photo_reference="
                    + photoReference + "&key=" + apiKey;
            if (photoUrl.length() <= 1000) {
                place.setIcon(photoUrl);
            } else {
                place.setIcon(result.optString("icon")); // Fallback to generic icon
            }
        } else {
            place.setIcon("https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/geocode-71.png"); // Default icon
        }

        // Save to database
        try {
            googlePlacesRepository.save(place);
        } catch (DataAccessException e) {
            System.err.println("Database error while saving place " + placeId + ": " + e.getMessage() + ". Returning API data without saving.");
        }

        return place;
    }
}