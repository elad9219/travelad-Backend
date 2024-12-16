package com.example.travelad.controller;

import com.example.travelad.beans.Attraction;
import com.example.travelad.service.GeoapifyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GeoapifyController {

    private final GeoapifyService geoapifyService;

    public GeoapifyController(GeoapifyService geoapifyService) {
        this.geoapifyService = geoapifyService;
    }

    @GetMapping("/api/geoapify/places")
    public List<Attraction> getPlaces(@RequestParam String city) {
        return geoapifyService.searchPlacesByCity(city);  // This will call the method from the service
    }
}
