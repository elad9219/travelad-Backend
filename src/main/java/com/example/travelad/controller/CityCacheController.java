package com.example.travelad.controller;

import com.example.travelad.service.CityCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/cache")
public class CityCacheController {

    private static final Logger logger = LoggerFactory.getLogger(CityCacheController.class);
    private final CityCacheService cityCacheService;

    @Autowired
    public CityCacheController(CityCacheService cityCacheService) {
        this.cityCacheService = cityCacheService;
    }

    /**
     * Retrieves the list of cached cities for a specific user.
     */
    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCachedCities(@RequestParam String userId) {
        try {
            List<String> cities = cityCacheService.getCities(userId);
            return ResponseEntity.ok(cities);
        } catch (Exception e) {
            logger.error("Error fetching cached cities for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).body(List.of());
        }
    }

    /**
     * Adds a city to the user's search history cache.
     */
    @PostMapping("/cities")
    public ResponseEntity<String> addCityToCache(@RequestParam String userId, @RequestParam String city) {
        try {
            String result = cityCacheService.addCity(userId, city);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error adding city {} to cache for user {}: {}", city, userId, e.getMessage());
            return ResponseEntity.status(500).body("Failed to add city to cache");
        }
    }

    /**
     * Removes a specific city from the user's search history cache.
     */
    @DeleteMapping("/cities")
    public ResponseEntity<String> removeCityFromCache(@RequestParam String userId, @RequestParam String city) {
        try {
            String result = cityCacheService.removeCity(userId, city);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error removing city {} from cache for user {}: {}", city, userId, e.getMessage());
            return ResponseEntity.status(500).body("Failed to remove city from cache");
        }
    }

    /**
     * Clears all cities from the user's search history cache.
     */
    @DeleteMapping("/cities/clear")
    public ResponseEntity<String> clearCities(@RequestParam String userId) {
        try {
            String result = cityCacheService.clearCities(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error clearing cities for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).body("Failed to clear cities");
        }
    }
}