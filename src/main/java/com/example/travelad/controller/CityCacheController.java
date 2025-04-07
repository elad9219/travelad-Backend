package com.example.travelad.controller;

import com.example.travelad.service.CityCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cache")
@CrossOrigin(origins = "*")
public class CityCacheController {

    @Autowired
    private CityCacheService cityCacheService;

    @GetMapping("/cities/autocomplete")
    public ResponseEntity<Object> autocompleteCities() {
        Object cities = cityCacheService.getCities();
        if (cities instanceof List && ((List<?>) cities).isEmpty()) {
            return ResponseEntity.ok("No cities in the list");
        }
        return ResponseEntity.ok(cities); // Return list of cities
    }

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
