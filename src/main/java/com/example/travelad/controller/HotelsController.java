package com.example.travelad.controller;

import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.HotelOfferSearch;
import com.example.travelad.dto.HotelDto;
import com.example.travelad.dto.HotelOffersDto;
import com.example.travelad.dto.RoomDto;
import com.example.travelad.exceptions.ExternalApiException;
import com.example.travelad.exceptions.InvalidInputException;
import com.example.travelad.service.HotelsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
    private static final Logger logger = LoggerFactory.getLogger(HotelsController.class);

    public HotelsController(HotelsService hotelsService) {
        this.hotelsService = hotelsService;
    }

    @GetMapping("/by-city-name")
    public ResponseEntity<?> getHotelsByCityName(@RequestParam String cityName) {
        try {
            if (cityName == null || cityName.isEmpty()) {
                return ResponseEntity.badRequest().body("City name is required.");
            }

            Hotel[] locations = hotelsService.searchHotelsByCityName(cityName);

            if (locations == null || locations.length == 0) {
                return ResponseEntity.notFound().build();
            }

            List<HotelDto> hotels = Arrays.stream(locations)
                    .filter(hotel -> hotel != null)
                    .map(location -> {
                        return new HotelDto(
                                location.getName() != null ? location.getName() : "Unknown",
                                location.getHotelId() != null ? location.getHotelId() : "Unknown",
                                location.getIataCode() != null ? location.getIataCode() : "Unknown",
                                location.getAddress() != null && location.getAddress().getCountryCode() != null ? location.getAddress().getCountryCode() : "Unknown"
                        );
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(hotels);

        } catch (Exception e) {
            logger.error("Failed to fetch hotels for city: {} - {}", cityName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while fetching hotels for " + cityName);
        }
    }

    // Endpoint to get hotels by IATA city code
    @GetMapping("/by-city-code")
    public ResponseEntity<?> getHotelsByCityCode(@RequestParam String cityCode) {
        try {
            if (cityCode == null || cityCode.isEmpty()) {
                return ResponseEntity.badRequest().body("City code is required.");
            }

            Hotel[] locations = hotelsService.searchHotelsByCityCode(cityCode);

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
            return ResponseEntity.internalServerError().body("Error fetching hotels by city code: " + e.getMessage());
        }
    }

    // Existing endpoint for hotel offers
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