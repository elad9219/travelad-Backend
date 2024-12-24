package com.example.travelad.controller;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.service.GooglePlacesService;
import com.example.travelad.service.CityCacheService;  // Import the CityCacheService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GooglePlacesController {

    private final GooglePlacesService googlePlacesService;
    private final CityCacheService cityCacheService; // Autowired CityCacheService

    @Autowired
    public GooglePlacesController(GooglePlacesService googlePlacesService, CityCacheService cityCacheService) {
        this.googlePlacesService = googlePlacesService;
        this.cityCacheService = cityCacheService;  // Inject CityCacheService
    }

    @GetMapping("/api/places/search")
    public GooglePlaces searchOrFetchPlace(@RequestParam String city) {
        // Fetch the place details from Google Places
        GooglePlaces place = googlePlacesService.searchPlaceByCity(city);

        // After finding the place, add the city to the cache
        cityCacheService.addCity(city);

        // Return the place details
        return place;
    }
}
