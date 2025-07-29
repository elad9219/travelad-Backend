package com.example.travelad.controller;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.service.GooglePlacesService;
import com.example.travelad.service.CityCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/places")
public class GooglePlacesController {

    private static final Logger logger = LoggerFactory.getLogger(GooglePlacesController.class);
    private final GooglePlacesService googlePlacesService;
    private final CityCacheService cityCacheService;

    @Autowired
    public GooglePlacesController(GooglePlacesService googlePlacesService, CityCacheService cityCacheService) {
        this.googlePlacesService = googlePlacesService;
        this.cityCacheService = cityCacheService;
    }

    @GetMapping("/search")
    public ResponseEntity<GooglePlaces> searchOrFetchPlace(@RequestParam String city, @RequestParam(required = false) String userId) {
        try {
            String decodedCity = URLDecoder.decode(city, "UTF-8");
            GooglePlaces place = googlePlacesService.searchPlaceByCity(decodedCity);
            // Cache city for user
            try {
                String effectiveUserId = userId != null ? userId : UUID.randomUUID().toString();
                cityCacheService.addCity(effectiveUserId, decodedCity);
            } catch (Exception e) {
                logger.warn("Failed to cache city {} for user {}: {}", decodedCity, userId, e.getMessage());
            }
            return ResponseEntity.ok(place);
        } catch (Exception e) {
            logger.error("Error searching for city {}: {}", city, e.getMessage());
            GooglePlaces fallbackPlace = new GooglePlaces();
            fallbackPlace.setCity(city);
            fallbackPlace.setName(city);
            fallbackPlace.setAddress("Unknown address");
            fallbackPlace.setIcon("https://via.placeholder.com/150");
            fallbackPlace.setLatitude(0.0);
            fallbackPlace.setLongitude(0.0);
            fallbackPlace.setPlaceId("unknown_" + city);
            fallbackPlace.setAttractionCount(0);
            fallbackPlace.setComplete(false);
            fallbackPlace.setCountry("");
            return ResponseEntity.status(HttpStatus.OK).body(fallbackPlace);
        }
    }

    @GetMapping("/cache/cities")
    public ResponseEntity<List<String>> getCachedCities(@RequestParam String userId) {
        try {
            List<String> cities = cityCacheService.getCities(userId);
            // Decode cities before returning
            List<String> decodedCities = cities.stream()
                    .map(city -> {
                        try {
                            return URLDecoder.decode(city, "UTF-8");
                        } catch (Exception e) {
                            return city;
                        }
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(decodedCities);
        } catch (Exception e) {
            logger.error("Error fetching cached cities for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @PostMapping("/cache/cities")
    public ResponseEntity<String> addCityToCache(@RequestParam String userId, @RequestParam String city) {
        try {
            String decodedCity = URLDecoder.decode(city, "UTF-8");
            String result = cityCacheService.addCity(userId, decodedCity);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error adding city {} to cache for user {}: {}", city, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add city to cache");
        }
    }

    @DeleteMapping("/cache/cities")
    public ResponseEntity<String> removeCityFromCache(@RequestParam String userId, @RequestParam String city) {
        try {
            String decodedCity = URLDecoder.decode(city, "UTF-8");
            String result = cityCacheService.removeCity(userId, decodedCity);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error removing city {} from cache for user {}: {}", city, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to remove city from cache");
        }
    }
}