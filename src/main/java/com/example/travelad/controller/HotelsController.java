package com.example.travelad.controller;

import com.example.travelad.dto.HotelDto;
import com.example.travelad.service.HotelsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<List<HotelDto>> getHotelsByCityName(
            @RequestParam String cityName,
            @RequestParam(required = false) String checkInDate,
            @RequestParam(required = false) String checkOutDate,
            @RequestParam(required = false) Integer adults) {

        return ResponseEntity.ok(hotelsService.searchHotelsByCityName(
                cityName, checkInDate, checkOutDate, adults));
    }

    @GetMapping("/offers")
    public ResponseEntity<List<HotelDto>> getHotelOffers(
            @RequestParam String hotelIds) {

        return ResponseEntity.ok(List.of());
    }
}