package com.example.travelad.controller;

import com.example.travelad.service.CityCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping({"/cache", "/api/places/cache"})
public class CityCacheController {

    private static final Logger logger = LoggerFactory.getLogger(CityCacheController.class);
    private final CityCacheService cityCacheService;

    public CityCacheController(CityCacheService cityCacheService) {
        this.cityCacheService = cityCacheService;
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCachedCities(
            @RequestParam("userId") String userId) {
        CityCacheService.validateUserId(userId);

        try {
            return ResponseEntity.ok(cityCacheService.getCities(userId));
        } catch (Exception e) {
            logger.warn("History read unavailable ({})", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(List.of());
        }
    }

    @PostMapping("/cities")
    public ResponseEntity<String> addCityToCache(
            @RequestParam("userId") String userId,
            @RequestParam("city") String city) {
        CityCacheService.validateUserId(userId);
        String normalizedCity = CityCacheService.normalizeCity(city);

        try {
            return ResponseEntity.ok(cityCacheService.addCity(userId, normalizedCity));
        } catch (Exception e) {
            logger.warn("History update unavailable ({})", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("History is unavailable");
        }
    }

    @DeleteMapping("/cities")
    public ResponseEntity<String> removeCityFromCache(
            @RequestParam("userId") String userId,
            @RequestParam("city") String city) {
        CityCacheService.validateUserId(userId);
        String normalizedCity = CityCacheService.normalizeCity(city);

        try {
            return ResponseEntity.ok(cityCacheService.removeCity(userId, normalizedCity));
        } catch (Exception e) {
            logger.warn("History removal unavailable ({})", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("History is unavailable");
        }
    }

    @DeleteMapping("/cities/clear")
    public ResponseEntity<String> clearCities(
            @RequestParam("userId") String userId) {
        CityCacheService.validateUserId(userId);

        try {
            return ResponseEntity.ok(cityCacheService.clearCities(userId));
        } catch (Exception e) {
            logger.warn("History clear unavailable ({})", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("History is unavailable");
        }
    }
}