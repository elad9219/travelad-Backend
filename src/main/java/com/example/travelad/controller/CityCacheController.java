package com.example.travelad.controller;

import com.example.travelad.service.CityCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cache")
public class CityCacheController {

    @Autowired
    private CityCacheService cityCacheService;

    // Get all cities in the cache for autocomplete
    @GetMapping("/cities/autocomplete")
    public ResponseEntity<Object> autocompleteCities() {
        Object cities = cityCacheService.getCities();
        if (cities instanceof String) {
            // Return custom message if it's a string
            return ResponseEntity.ok(cities);
        }
        return ResponseEntity.ok(cities); // Return list of cities
    }

    // Add a city to the cache
    @PostMapping("/cities")
    public ResponseEntity<String> addCity(@RequestParam String city) {
        String message = cityCacheService.addCity(city);
        if (message.equals("City added successfully")) {
            return ResponseEntity.ok(message);
        }
        return ResponseEntity.status(400).body(message); // Return error if city already exists
    }

    // Remove a specific city from the cache and return appropriate message
    @DeleteMapping("/cities")
    public ResponseEntity<String> removeCity(@RequestParam String city) {
        String message = cityCacheService.removeCity(city);
        if (message.equals("City removed successfully")) {
            return ResponseEntity.ok(message);  // Return success message
        }
        return ResponseEntity.status(404).body(message); // Return error if city doesn't exist
    }

    // Clear all cities from the cache and return a success message
    @DeleteMapping("/cities/clear")
    public ResponseEntity<String> clearCities() {
        String message = cityCacheService.clearCities();
        return ResponseEntity.ok(message);  // Send success message
    }
}
