package com.example.travelad.service;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.repositories.GooglePlacesRepository;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class GooglePlacesService {

    private static final Logger logger = LoggerFactory.getLogger(GooglePlacesService.class);
    private final GooglePlacesRepository googlePlacesRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.places.api.key}")
    private String apiKey;

    public GooglePlacesService(GooglePlacesRepository googlePlacesRepository) {
        this.googlePlacesRepository = googlePlacesRepository;
    }

    /**
     * Searches for GooglePlaces data for the given city.
     * If a record exists and is marked as complete, returns it from the database.
     * Otherwise, calls the API and returns immediate data while asynchronously marking the record as complete.
     */
    public GooglePlaces searchPlaceByCity(String cityName) {
        String normalizedCity = cityName.toLowerCase();
        List<GooglePlaces> cachedPlaces = null;
        try {
            cachedPlaces = googlePlacesRepository.findByCityIgnoreCase(normalizedCity);
            if (cachedPlaces != null && !cachedPlaces.isEmpty()) {
                GooglePlaces place = cachedPlaces.get(0);
                if (place.isComplete()) {
                    logger.info("Returning cached GooglePlaces for city: {}", normalizedCity);
                    return place;
                }
            }
        } catch (DataAccessException e) {
            logger.error("Database error while fetching places for city {}: {}. Falling back to API.", normalizedCity, e.getMessage());
        }

        // Not found or not complete; call API
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + normalizedCity + "&key=" + apiKey;
        String response = restTemplate.getForObject(url, String.class);
        JSONObject responseJson = new JSONObject(response);
        if (responseJson.has("results") && responseJson.getJSONArray("results").length() > 0) {
            JSONObject firstResult = responseJson.getJSONArray("results").getJSONObject(0);
            GooglePlaces place = savePlaceFromApiResponse(firstResult, normalizedCity);
            // Asynchronously mark the record as complete
            asyncMarkGooglePlaceComplete(place);
            return place;
        }
        return null;
    }

    private GooglePlaces savePlaceFromApiResponse(JSONObject result, String cityName) {
        String placeId = result.getString("place_id");
        Optional<GooglePlaces> existingPlace;
        try {
            existingPlace = googlePlacesRepository.findByPlaceId(placeId);
        } catch (DataAccessException e) {
            logger.error("Database error while checking existing place {}: {}. Proceeding without DB check.", placeId, e.getMessage());
            existingPlace = Optional.empty();
        }
        GooglePlaces place = existingPlace.orElseGet(GooglePlaces::new);
        place.setPlaceId(placeId);
        place.setName(result.optString("name"));
        place.setAddress(result.optString("formatted_address"));
        place.setLatitude(result.getJSONObject("geometry").getJSONObject("location").getDouble("lat"));
        place.setLongitude(result.getJSONObject("geometry").getJSONObject("location").getDouble("lng"));
        place.setCity(cityName);
        if (result.has("photos")) {
            JSONObject photo = result.getJSONArray("photos").getJSONObject(0);
            String photoReference = photo.getString("photo_reference");
            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=3400&photo_reference="
                    + photoReference + "&key=" + apiKey;
            place.setIcon(photoUrl);
        } else {
            place.setIcon("https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/geocode-71.png");
        }
        try {
            googlePlacesRepository.save(place);
        } catch (DataAccessException e) {
            logger.error("Database error while saving GooglePlaces {}: {}. Returning API data without saving.", placeId, e.getMessage());
        }
        return place;
    }

    @Async
    public void asyncMarkGooglePlaceComplete(GooglePlaces place) {
        try {
            place.setComplete(true);
            googlePlacesRepository.save(place);
            logger.info("Marked GooglePlaces for city {} as complete.", place.getCity());
        } catch (DataAccessException e) {
            logger.error("Error marking GooglePlaces as complete for city {}: {}", place.getCity(), e.getMessage());
        }
    }
}
