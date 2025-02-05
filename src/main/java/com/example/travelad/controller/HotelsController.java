package com.example.travelad.controller;

import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.HotelOfferSearch;
import com.example.travelad.dto.HotelDto;
import com.example.travelad.dto.HotelOffersDto;
import com.example.travelad.dto.RoomDto;
import com.example.travelad.service.HotelsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

            // Search hotels by city name
            com.amadeus.resources.Hotel[] locations = hotelsService.searchHotelsByCityName(cityName);

            if (locations == null || locations.length == 0) {
                return ResponseEntity.notFound().build();
            }

            List<HotelDto> hotels = Arrays.stream(locations)
                    .filter(Objects::nonNull)
                    .map(location -> new HotelDto(
                            location.getName() != null ? location.getName() : "Unknown",
                            location.getHotelId() != null ? location.getHotelId() : "Unknown",
                            location.getIataCode() != null ? location.getIataCode() : "Unknown",
                            (location.getAddress() != null && location.getAddress().getCountryCode() != null)
                                    ? location.getAddress().getCountryCode()
                                    : "Unknown"
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(hotels);

        } catch (Exception e) {
            logger.error("Failed to fetch hotels for city: {} - {}", cityName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching hotels for " + cityName);
        }
    }

    @GetMapping("/by-city-code")
    public ResponseEntity<?> getHotelsByCityCode(@RequestParam String cityCode) {
        try {
            if (cityCode == null || cityCode.isEmpty()) {
                return ResponseEntity.badRequest().body("City code is required.");
            }

            com.amadeus.resources.Hotel[] locations = hotelsService.searchHotelsByCityCode(cityCode);

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
            return ResponseEntity.internalServerError()
                    .body("Error fetching hotels by city code: " + e.getMessage());
        }
    }

    @GetMapping("/offers")
    public ResponseEntity<?> getHotelOffers(
            @RequestParam String hotelIds,
            @RequestParam(required = false) String checkInDate,
            @RequestParam(required = false) String checkOutDate,
            @RequestParam(required = false) Integer adults
    ) {
        try {
            HotelOfferSearch[] offers = hotelsService.searchHotelOffers(hotelIds, checkInDate, checkOutDate, adults);
            if (offers == null || offers.length == 0) {
                logger.info("No hotel offers returned for hotelIds: {}", hotelIds);
                return ResponseEntity.ok(List.of());
            }

            // Process offers safely and skip any offer where required fields are missing.
            List<HotelOffersDto> hotelOffers = Arrays.stream(offers)
                    .filter(Objects::nonNull)
                    .flatMap(offer -> {
                        if (offer.getHotel() == null || offer.getOffers() == null) {
                            logger.info("Hotel or Offers are null for offer: {}", offer);
                            return Stream.empty();
                        }
                        // For each offer, process each individual hotelOffer entry
                        return Arrays.stream(offer.getOffers())
                                .filter(hotelOffer ->
                                        hotelOffer != null &&
                                                hotelOffer.getRoom() != null &&
                                                hotelOffer.getPrice() != null &&
                                                hotelOffer.getCheckInDate() != null &&
                                                hotelOffer.getCheckOutDate() != null &&
                                                offer.getHotel().getName() != null &&
                                                offer.getHotel().getCityCode() != null &&
                                                hotelOffer.getPrice().getCurrency() != null &&
                                                hotelOffer.getPrice().getBase() != null &&
                                                hotelOffer.getPrice().getTotal() != null
                                )
                                .map(hotelOffer -> {
                                    HotelOfferSearch.RoomDetails roomDetails = hotelOffer.getRoom();
                                    HotelOfferSearch.HotelPrice price = hotelOffer.getPrice();

                                    // If any nested field is missing, skip the offer by returning null
                                    if (roomDetails.getTypeEstimated() == null ||
                                            roomDetails.getTypeEstimated().getBedType() == null ||
                                            roomDetails.getTypeEstimated().getBeds() == null ||
                                            roomDetails.getDescription() == null ||
                                            roomDetails.getDescription().getText() == null) {
                                        return null;
                                    }

                                    RoomDto room = new RoomDto(
                                            roomDetails.getTypeEstimated().getBedType(),
                                            roomDetails.getTypeEstimated().getBeds(),
                                            roomDetails.getDescription().getText()
                                    );

                                    return new HotelOffersDto(
                                            offer.getHotel().getName(),
                                            offer.getHotel().getCityCode(),
                                            price.getCurrency(),
                                            price.getBase(),
                                            price.getTotal(),
                                            hotelOffer.getCheckInDate(),
                                            hotelOffer.getCheckOutDate(),
                                            room
                                    );
                                });
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(hotelOffers);
        } catch (Exception e) {
            logger.error("Failed to fetch hotel offers for hotelIds: {} - {}", hotelIds, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while fetching hotel offers.");
        }
    }
}
