package com.example.travelad.controller;

import com.example.travelad.service.HotelsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/hotels")
public class HotelsController {

    @Autowired
    private HotelsService hotelsService;

    // Search hotels by city code
    @GetMapping("/search/by-city")
    public ResponseEntity<String> searchHotelsByCity(
            @RequestParam String cityCode,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) String radiusUnit,
            @RequestParam(required = false) String chainCodes,
            @RequestParam(required = false) String amenities,
            @RequestParam(required = false) String ratings) {

        // Call service to fetch hotel data
        String hotelsData = hotelsService.searchHotelsByCity(cityCode, radius, radiusUnit, chainCodes, amenities, ratings);

        return ResponseEntity.ok(hotelsData);  // Return raw JSON response from Amadeus
    }
}
