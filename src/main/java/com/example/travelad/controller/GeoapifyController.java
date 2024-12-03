package com.example.travelad.controller;

import com.example.travelad.service.GeoapifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/places")
public class GeoapifyController {

    private final GeoapifyService geoapifyService;

    @Autowired
    public GeoapifyController(GeoapifyService geoapifyService) {
        this.geoapifyService = geoapifyService;
    }

    @GetMapping
    public ResponseEntity<?> getPlaces(@RequestParam String city) {
        try {
            String places = geoapifyService.findAttractionsByCity(city);
            return ResponseEntity.ok(places);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error fetching places: " + e.getMessage());
        }
    }
}
