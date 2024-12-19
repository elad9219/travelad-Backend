package com.example.travelad.service;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.repositories.GooglePlacesRepository;
import kong.unirest.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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

    public List<GooglePlaces> searchPlacesByCity(String cityName) {
        // Check if city attractions are already cached in the database
        List<GooglePlaces> cachedPlaces = googlePlacesRepository.findByCityIgnoreCase(cityName);
        if (!cachedPlaces.isEmpty()) {
            return cachedPlaces;
        }

        // If not, fetch places from Google Places API
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" + cityName + "&key=" + apiKey;
        String response = restTemplate.getForObject(url, String.class);

        // Parse API response
        JSONObject responseJson = new JSONObject(response);
        return parsePlaces(responseJson, cityName);
    }

    private List<GooglePlaces> parsePlaces(JSONObject responseJson, String cityName) {
        List<GooglePlaces> places = new ArrayList<>();
        responseJson.getJSONArray("results").forEach(resultObj -> {
            JSONObject result = (JSONObject) resultObj;
            String placeId = result.getString("place_id");

            // Check if the placeId already exists in the database
            Optional<GooglePlaces> existingPlace = googlePlacesRepository.findByPlaceId(placeId);
            GooglePlaces place;
            if (existingPlace.isPresent()) {
                place = existingPlace.get();
            } else {
                place = new GooglePlaces();
                place.setPlaceId(placeId);
            }

            // Update fields for both new and existing records
            place.setName(result.optString("name"));
            place.setAddress(result.optString("formatted_address"));
            place.setLatitude(result.getJSONObject("geometry").getJSONObject("location").getDouble("lat"));
            place.setLongitude(result.getJSONObject("geometry").getJSONObject("location").getDouble("lng"));
            place.setCity(cityName);

            // Always update the icon field with the photo URL if available
            if (result.has("photos")) {
                JSONObject photo = result.getJSONArray("photos").getJSONObject(0);
                String photoReference = photo.getString("photo_reference");
                String photoUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference="
                        + photoReference + "&key=" + apiKey;
                if (photoUrl.length() <= 1000) {
                    place.setIcon(photoUrl);
                } else {
                    place.setIcon(result.optString("icon")); // Fallback to generic icon if too long
                }
            }

            googlePlacesRepository.save(place);
            places.add(place);
        });
        return places;
    }

}
