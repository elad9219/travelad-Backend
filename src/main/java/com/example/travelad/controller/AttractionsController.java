package com.example.travelad.controller;

import com.example.travelad.beans.Attraction;
import com.example.travelad.service.AttractionsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AttractionsController {

    private final AttractionsService attractionsService;

    public AttractionsController(AttractionsService attractionsService) {
        this.attractionsService = attractionsService;
    }

    @GetMapping("/api/geoapify/places")
    public List<Attraction> getPlaces(@RequestParam String city) {
        return attractionsService.searchPlacesByCity(city);  // This will call the method from the service
    }
}
