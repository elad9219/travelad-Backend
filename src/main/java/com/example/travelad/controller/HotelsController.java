package com.example.travelad.controller;

// HotelsController.java
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.example.travelad.service.HotelsService;

@RestController
@RequestMapping("/api/hotels")
public class HotelsController {

    @Autowired
    private HotelsService hotelsService;  // Inject the HotelsService

    @GetMapping("/search")
    public ResponseEntity<?> searchHotels(
            @RequestParam String cityName,
            @RequestParam int page,
            @RequestParam String currency,
            @RequestParam int numOfRooms,
            @RequestParam int numOfAdults,
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate) {

        // Call the hotelsService's searchHotels method
        ResponseEntity<?> response = hotelsService.searchHotels(cityName, page, currency, numOfRooms, numOfAdults, checkInDate, checkOutDate);
        return ResponseEntity.ok(response.getBody());
    }
}
