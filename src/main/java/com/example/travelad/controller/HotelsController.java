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
    public ResponseEntity<?> getHotelsByCityName(@RequestParam String cityName) {
        if (cityName == null || cityName.isEmpty()) {
            return ResponseEntity.badRequest().body("City name is required.");
        }

        // הסרוויס מחזיר ישר DTOs עם מחירים
        List<HotelDto> hotels = hotelsService.searchHotelsByCityName(cityName);

        if (hotels.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(hotels);
    }

    // משאירים את ה-Endpoint הזה קיים כדי שהפרונטאנד לא יקרוס, אבל הוא יחזיר רשימה ריקה
    // כי המחירים כבר מגיעים ב-Endpoint הראשון
    @GetMapping("/offers")
    public ResponseEntity<?> getHotelOffers(@RequestParam String hotelIds) {
        return ResponseEntity.ok(List.of());
    }
}