package com.example.travelad.controller;

import com.amadeus.resources.FlightOfferSearch;
import com.example.travelad.service.FlightsService;
import com.example.travelad.utils.IataCodeUtils;
import com.amadeus.exceptions.ResponseException;
import com.example.travelad.dto.FlightOfferDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/flights")
public class FlightsController {

    private final FlightsService flightsService;

    public FlightsController(FlightsService flightsService) {
        this.flightsService = flightsService;
    }

    @GetMapping
    public ResponseEntity<?> flights(@RequestParam String city) {
        String iataCode = IataCodeUtils.getIataCodeForCity(city);
        if (iataCode == null) {
            return ResponseEntity.ok(List.of()); // Return an empty list if no IATA code is found
        }

        try {
            String origin = "TLV"; // Replace with your origin
            String departDate = LocalDate.now().plusDays(10).toString();
            String returnDate = LocalDate.now().plusDays(15).toString();
            String adults = "1";

            FlightOfferSearch[] flightOffers = flightsService.flights(origin, iataCode, departDate, adults, returnDate);
            if (flightOffers == null || flightOffers.length == 0) {
                return ResponseEntity.ok(List.of()); // Return an empty list if no flights are found
            }

            List<FlightOfferDto> flightOfferDtos = Arrays.stream(flightOffers)
                    .map(FlightOfferDto::fromFlightOfferSearch)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(flightOfferDtos);
        } catch (ResponseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching flights: " + e.getMessage());
        }
    }
}
