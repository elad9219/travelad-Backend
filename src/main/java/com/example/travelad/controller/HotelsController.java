package com.example.travelad.controller;

import com.example.travelad.dto.HotelDto;
import com.example.travelad.service.HotelsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hotels")
@CrossOrigin(origins = "*")
public class HotelsController {

    private final HotelsService hotelsService;

    public HotelsController(HotelsService hotelsService) {
        this.hotelsService = hotelsService;
    }

    @GetMapping("/by-city-name")
    public ResponseEntity<?> getHotelsByCityName(
            @RequestParam String cityName,
            @RequestParam(required = false) String checkInDate,
            @RequestParam(required = false) String checkOutDate,
            @RequestParam(required = false) Integer adults) {

        if (cityName == null || cityName.isEmpty()) {
            return ResponseEntity.badRequest().body("City name is required.");
        }

        // וידוא שהערך של האורחים תקין (מינימום 1)
        Integer numAdults = (adults != null && adults > 0) ? adults : 1;

        // קריאה לסרוויס עם כל 4 הפרמטרים הנדרשים
        List<HotelDto> hotels = hotelsService.searchHotelsByCityName(cityName, checkInDate, checkOutDate, numAdults);

        if (hotels.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(hotels);
    }

    @GetMapping("/offers")
    public ResponseEntity<?> getHotelOffers(@RequestParam String hotelIds) {
        return ResponseEntity.ok(List.of());
    }
}