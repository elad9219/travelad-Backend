package com.example.travelad.controller;

import com.example.travelad.service.GooglePlacesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlacesController {

    @Autowired
    public GooglePlacesService googlePlacesService;

    @GetMapping("/api/places/search")
    public String searchPlaces(@RequestParam String query) {
        return googlePlacesService.searchPlace(query);
    }

    @GetMapping("/api/places/details")
    public String getPlaceDetails(@RequestParam String placeId) {
        return googlePlacesService.getPlaceDetails(placeId);
    }
}

