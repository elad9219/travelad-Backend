package com.example.travelad.controller;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.service.CityCacheService;
import com.example.travelad.service.GooglePlacesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/places")
public class GooglePlacesController {

    private static final Logger logger = LoggerFactory.getLogger(GooglePlacesController.class);
    private final GooglePlacesService googlePlacesService;
    private final CityCacheService cityCacheService;

    public GooglePlacesController(
            GooglePlacesService googlePlacesService,
            CityCacheService cityCacheService) {
        this.googlePlacesService = googlePlacesService;
        this.cityCacheService = cityCacheService;
    }

    @GetMapping("/search")
    public ResponseEntity<GooglePlaces> searchOrFetchPlace(
            @RequestParam("city") String city,
            @RequestParam(value = "userId", required = false) String userId) {
        // Spring has already decoded query parameters. Preserve literal '+' and '%'.
        String normalizedCity = CityCacheService.normalizeCity(city);
        if (userId != null) {
            CityCacheService.validateUserId(userId);
        }

        GooglePlaces place;
        try {
            place = googlePlacesService.searchPlaceByCity(normalizedCity);
        } catch (Exception e) {
            logger.warn("Place search unavailable ({})", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }

        if (place == null ||
                place.getPlaceId() == null ||
                place.getPlaceId().isBlank() ||
                place.getPlaceId().startsWith("unknown_")) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }

        // Missing history identifiers must not create unreachable anonymous keys.
        if (userId != null) {
            try {
                cityCacheService.addCity(userId, normalizedCity);
            } catch (Exception e) {
                // A history outage must not discard valid place details.
                logger.warn(
                        "Place loaded but history update unavailable ({})",
                        e.getClass().getSimpleName()
                );
            }
        }

        return ResponseEntity.ok(place);
    }
}