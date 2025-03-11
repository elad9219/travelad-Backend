package com.example.travelad.controller;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.service.GooglePlacesService;
import com.example.travelad.service.CityCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GooglePlacesController {

    private final GooglePlacesService googlePlacesService;
    private final CityCacheService cityCacheService;

    @Autowired
    public GooglePlacesController(GooglePlacesService googlePlacesService, CityCacheService cityCacheService) {
        this.googlePlacesService = googlePlacesService;
        this.cityCacheService = cityCacheService;
    }

    @GetMapping("/api/places/search")
    public ResponseEntity<GooglePlaces> searchOrFetchPlace(@RequestParam String city) {
        GooglePlaces place = googlePlacesService.searchPlaceByCity(city);
        if (place == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Or a custom error object
        }
        cityCacheService.addCity(city);
        return ResponseEntity.ok(place);
    }
}