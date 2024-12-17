package com.example.travelad.controller;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.service.GooglePlacesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GooglePlacesController {

    private final GooglePlacesService googlePlacesService;

    @Autowired
    public GooglePlacesController(GooglePlacesService googlePlacesService) {
        this.googlePlacesService = googlePlacesService;
    }

    @GetMapping("/api/places/search")
    public List<GooglePlaces> searchOrFetchPlace(@RequestParam String city) {
        return googlePlacesService.searchPlacesByCity(city);
    }
}
