package com.example.travelad.controller;

import com.example.travelad.beans.Attraction;
import com.example.travelad.service.AttractionsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class AttractionsController {

    private final AttractionsService attractionsService;

    public AttractionsController(AttractionsService attractionsService) {
        this.attractionsService = attractionsService;
    }

    @GetMapping("/api/geoapify/places")
    public ResponseEntity<List<Attraction>> getPlaces(@RequestParam String city) {
        List<Attraction> attractions = attractionsService.searchPlacesByCity(city);
        if (attractions == null || attractions.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(attractions);
    }
}
