package com.example.travelad.controller;

import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.HotelOfferSearch;
import com.example.travelad.beans.HotelDto;
import com.example.travelad.beans.HotelOffersDto;
import com.example.travelad.beans.RoomDto;
import com.example.travelad.exceptions.ExternalApiException;
import com.example.travelad.exceptions.InvalidInputException;
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

    // Endpoint to get hotels by city code
    @GetMapping("/by-city")
    public ResponseEntity<?> getHotelsByCity(@RequestParam String cityCode) {
        try {
            if (cityCode == null || cityCode.isEmpty()) {
                return ResponseEntity.badRequest().body("City code is required.");
            }

            Hotel[] locations = hotelsService.searchHotelsByCity(cityCode);

            if (locations == null || locations.length == 0) {
                return ResponseEntity.notFound().build();
            }

            List<HotelDto> hotels = Arrays.stream(locations)
                    .map(location -> new HotelDto(
                            location.getName(),
                            location.getHotelId(),
                            location.getIataCode(),
                            location.getAddress().getCountryCode()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(hotels);

        } catch (ResponseException e) {
            return ResponseEntity.internalServerError().body("Error fetching hotels: " + e.getMessage());
        }
    }




    // Endpoint to get hotel offers
    @GetMapping("/offers")
    public ResponseEntity<?> getHotelOffers(
            @RequestParam String hotelIds,
            @RequestParam(required = false) String checkInDate,
            @RequestParam(required = false) String checkOutDate,
            @RequestParam(required = false) Integer adults
    ) {
        try {
            List<HotelOffersDto> hotels = Arrays.stream(hotelsService.searchHotelOffers(hotelIds, checkInDate, checkOutDate, adults))
                    .map(offer -> {
                        HotelOfferSearch.RoomDetails roomDetails = offer.getOffers()[0].getRoom();

                        RoomDto room = new RoomDto(
                                roomDetails.getTypeEstimated().getBedType(),
                                roomDetails.getTypeEstimated().getBeds(),
                                roomDetails.getDescription().getText()
                        );

                        return new HotelOffersDto(
                                offer.getHotel().getName(),
                                offer.getHotel().getCityCode(),
                                offer.getOffers()[0].getPrice().getCurrency(),
                                offer.getOffers()[0].getPrice().getBase(),
                                offer.getOffers()[0].getPrice().getTotal(),
                                offer.getOffers()[0].getCheckInDate(),
                                offer.getOffers()[0].getCheckOutDate(),
                                room
                        );
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(hotels);

        } catch (InvalidInputException e) {
            throw e; // Caught by GlobalExceptionHandler
        } catch (ExternalApiException e) {
            throw e; // Caught by GlobalExceptionHandler
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occurred while processing hotel offers.", e);
        }
    }
}
