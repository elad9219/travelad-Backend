package com.example.travelad.controller;

import com.example.travelad.beans.Attraction;
import com.example.travelad.service.AttractionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class AttractionsController {
    private final AttractionsService service;

    public AttractionsController(AttractionsService service) {
        this.service = service;
    }

    @GetMapping("/api/geoapify/places")
    public ResponseEntity<List<Attraction>> getPlaces(@RequestParam String city) {
        try {
            return ResponseEntity.ok(service.searchPlacesByCity(city));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(502).build();
        }
    }
}