package com.example.travelad.controller;

import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.HotelOfferSearch;
import com.example.travelad.beans.HotelDto;
import com.example.travelad.service.HotelsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.amadeus.resources.Hotel;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/hotels")
public class HotelsController {

    private final HotelsService hotelsService;

    public HotelsController(HotelsService hotelsService) {
        this.hotelsService = hotelsService;
    }


    @GetMapping("/by-city")
    public ResponseEntity<?> getHotelsByCity(@RequestParam String cityCode) {
        try {
            Hotel[] locations = hotelsService.searchHotelsByCity(cityCode);

            if (locations == null || locations.length == 0) {
                return ResponseEntity.notFound().build();
            }

            List<HotelDto> hotels = Arrays.stream(locations)
                    .map(location -> new HotelDto(
                            location.getName(),
                            cityCode,
                            "Unknown",
                            "N/A",
                            "Amadeus"
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(hotels);

        } catch (ResponseException e) {
            return ResponseEntity.internalServerError().body("Error fetching hotels: " + e.getMessage());
        }
    }





    @GetMapping("/offers")
    public ResponseEntity<?> getHotelOffers(
            @RequestParam String hotelIds,
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate,
            @RequestParam int adults
    ) {
        try {
            HotelOfferSearch[] hotelOffers = hotelsService.searchHotelOffers(hotelIds, checkInDate, checkOutDate, adults);

            if (hotelOffers == null || hotelOffers.length == 0) {
                return ResponseEntity.notFound().build();
            }

            List<HotelDto> hotels = Arrays.stream(hotelOffers)
                    .map(offer -> new HotelDto(
                            offer.getHotel().getName(),
                            offer.getHotel().getCityCode(),
                            offer.getHotel().getChainCode(),
                            offer.getOffers()[0].getPrice().getTotal(),
                            offer.getOffers()[0].getRateCode()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(hotels);

        } catch (ResponseException e) {
            return ResponseEntity.internalServerError().body("Error fetching hotel offers: " + e.getMessage());
        }
    }
}
