package com.example.travelad.service;

import com.example.travelad.beans.Attraction;
import com.example.travelad.beans.AttractionCacheStatus;
import com.example.travelad.dto.AttractionDto;
import com.example.travelad.repositories.AttractionCacheStatusRepository;
import com.example.travelad.repositories.AttractionRepository;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttractionsService {

    private static final Logger logger = LoggerFactory.getLogger(AttractionsService.class);

    private final RestTemplate restTemplate;
    private final AttractionRepository attractionRepository;
    private final AttractionCacheStatusRepository cacheStatusRepository;

    @Value("${geoapify.api.key}")
    private String apiKey;

    private String geocodingUrl;
    private String placesUrl;

    public AttractionsService(RestTemplate restTemplate, AttractionRepository attractionRepository,
                              AttractionCacheStatusRepository cacheStatusRepository) {
        this.restTemplate = restTemplate;
        this.attractionRepository = attractionRepository;
        this.cacheStatusRepository = cacheStatusRepository;
    }

    @PostConstruct
    public void init() {
        geocodingUrl = "https://api.geoapify.com/v1/geocode/search";
        placesUrl = "https://api.geoapify.com/v2/places";
    }

    private String normalizeCityName(String cityName) {
        if (cityName == null) return null;
        return cityName.split("-")[0].trim().toLowerCase();
    }

    /**
     * Searches for attractions by city.
     * If the persistent cache status is marked as complete, returns data from DB.
     * Otherwise, calls the API immediately and asynchronously saves attractions,
     * updating the persistent cache status.
     */
    public List<Attraction> searchPlacesByCity(String cityName) {
        String normalizedCity = normalizeCityName(cityName);
        boolean cacheComplete = cacheStatusRepository.findById(normalizedCity)
                .map(AttractionCacheStatus::isComplete)
                .orElse(false);
        if (cacheComplete) {
            List<Attraction> cachedAttractions = attractionRepository.findByCityIgnoreCase(normalizedCity);
            if (cachedAttractions != null && !cachedAttractions.isEmpty()) {
                logger.info("Returning cached attractions for city: {}", normalizedCity);
                return cachedAttractions;
            }
        }
        logger.info("Fetching attractions from Geoapify API for city: {}", normalizedCity);
        List<AttractionDto> geoapifyPlaces = fetchPlacesFromGeoapify(normalizedCity);
        List<Attraction> attractions = geoapifyPlaces.stream()
                .map(dto -> {
                    Attraction attraction = mapToAttraction(dto);
                    attraction.setCity(normalizedCity);
                    return attraction;
                })
                .filter(a -> a.getName() != null && !a.getName().isEmpty())
                .collect(Collectors.toList());
        // Save attraction cache status as incomplete first
        cacheStatusRepository.save(new AttractionCacheStatus(normalizedCity, false));
        // Asynchronously save attractions
        asyncSaveAttractions(attractions, normalizedCity);
        return attractions;
    }

    @Async
    public void asyncSaveAttractions(List<Attraction> attractions, String city) {
        boolean allSaved = true;
        if (attractions != null) {
            for (Attraction attraction : attractions) {
                try {
                    Optional<Attraction> existing = attractionRepository.findByNameAndCityIgnoreCase(attraction.getName(), attraction.getCity());
                    if (existing.isEmpty()) {
                        attractionRepository.save(attraction);
                        logger.info("Saved attraction asynchronously: {} in {}", attraction.getName(), attraction.getCity());
                    } else {
                        logger.info("Attraction already exists: {} in {}", attraction.getName(), attraction.getCity());
                    }
                } catch (DataAccessException e) {
                    allSaved = false;
                    logger.error("Error saving attraction {} asynchronously: {}", attraction.getName(), e.getMessage());
                }
            }
        }
        try {
            if (allSaved) {
                cacheStatusRepository.save(new AttractionCacheStatus(city, true));
                logger.info("Attraction cache for city {} marked as complete.", city);
            } else {
                logger.warn("Attraction cache for city {} remains incomplete.", city);
            }
        } catch (Exception e) {
            logger.error("Error updating attraction cache status for city {}: {}", city, e.getMessage());
        }
    }

    private List<AttractionDto> fetchPlacesFromGeoapify(String cityName) {
        String geocodingRequestUrl = String.format("%s?text=%s&apiKey=%s", geocodingUrl, cityName, apiKey);
        String geocodingResponse = restTemplate.getForObject(geocodingRequestUrl, String.class);
        JSONObject geocodingJson = new JSONObject(geocodingResponse);
        JSONArray features = geocodingJson.getJSONArray("features");
        if (features.isEmpty()) {
            throw new RuntimeException("City not found in Geoapify geocoding API");
        }
        JSONObject geometry = features.getJSONObject(0).getJSONObject("geometry");
        String lon = geometry.getJSONArray("coordinates").get(0).toString();
        String lat = geometry.getJSONArray("coordinates").get(1).toString();
        String placesRequestUrl = String.format("%s?categories=tourism.sights&filter=circle:%s,%s,5000&apiKey=%s&lang=en",
                placesUrl, lon, lat, apiKey);
        String placesResponse = restTemplate.getForObject(placesRequestUrl, String.class);
        return parsePlaces(new JSONObject(placesResponse));
    }

    private List<AttractionDto> parsePlaces(JSONObject placesJson) {
        List<AttractionDto> places = new ArrayList<>();
        JSONArray features = placesJson.getJSONArray("features");
        for (int i = 0; i < features.length(); i++) {
            JSONObject properties = features.getJSONObject(i).getJSONObject("properties");
            String name = properties.optJSONObject("name_international") != null
                    ? properties.getJSONObject("name_international").optString("en", properties.optString("name", null))
                    : properties.optString("name", null);
            if (name == null || name.isEmpty()) {
                logger.warn("Skipping attraction without a name at index {}", i);
                continue;
            }
            AttractionDto place = new AttractionDto(
                    name,
                    properties.optString("city", null),
                    properties.optString("country", null),
                    properties.optString("description", null)
            );
            place.setAddress(properties.optString("formatted", null));
            place.setPhone(properties.optJSONObject("contact") != null
                    ? properties.getJSONObject("contact").optString("phone", null)
                    : null);
            place.setWebsite(properties.optString("website", null));
            place.setOpening_hours(properties.optString("opening_hours", null));
            places.add(place);
        }
        return places;
    }

    private Attraction mapToAttraction(AttractionDto dto) {
        Attraction attraction = new Attraction();
        attraction.setName(dto.getName());
        attraction.setCity(dto.getCity());
        attraction.setCountry(dto.getCountry());
        attraction.setDescription(dto.getDescription());
        attraction.setAddress(dto.getAddress());
        attraction.setPhone(dto.getPhone());
        attraction.setWebsite(dto.getWebsite());
        attraction.setOpeningHours(dto.getOpening_hours());
        return attraction;
    }
}
