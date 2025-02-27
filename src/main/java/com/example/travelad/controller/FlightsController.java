package com.example.travelad.controller;

import com.amadeus.resources.FlightOfferSearch;
import com.amadeus.exceptions.ResponseException;
import com.example.travelad.dto.FlightOfferDto;
import com.example.travelad.service.FlightsService;
import com.example.travelad.utils.IataCodeUtils;
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
        final String destinationIata = IataCodeUtils.getIataCodeForCity(city);
        if (destinationIata == null) {
            return ResponseEntity.ok(List.of());
        }
        final String finalDestinationIata = (IataCodeUtils.getFinalDestinationIataForCity(city) != null)
                ? IataCodeUtils.getFinalDestinationIataForCity(city)
                : destinationIata;
        try {
            String origin = "TLV";
            String departDate = LocalDate.now().plusDays(10).toString();
            String returnDate = LocalDate.now().plusDays(15).toString();
            String adults = "1";
            FlightOfferSearch[] offers = flightsService.flights(origin, destinationIata, departDate, adults, returnDate);
            if (offers == null || offers.length == 0) {
                return ResponseEntity.ok(List.of());
            }
            List<FlightOfferDto> flightOfferDtos = Arrays.stream(offers)
                    .map(offer -> FlightOfferDto.fromFlightOfferSearch(offer, finalDestinationIata))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(flightOfferDtos);
        } catch (ResponseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching flights: " + e.getMessage());
        }
    }

    @GetMapping("/advancedFlightSearch")
    public ResponseEntity<?> advancedFlightSearch(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String departDate,
            @RequestParam(required = false, defaultValue = "") String returnDate,
            @RequestParam String adults) {
        final String originIata = IataCodeUtils.getIataCodeForCity(origin);
        final String destinationIata = IataCodeUtils.getIataCodeForCity(destination);
        if (originIata == null || destinationIata == null) {
            return ResponseEntity.badRequest().body("Invalid origin or destination city provided.");
        }
        final String finalDestinationIata = (IataCodeUtils.getFinalDestinationIataForCity(destination) != null)
                ? IataCodeUtils.getFinalDestinationIataForCity(destination)
                : destinationIata;
        try {
            FlightOfferSearch[] offers = flightsService.flights(originIata, destinationIata, departDate, adults, returnDate);
            if (offers == null || offers.length == 0) {
                return ResponseEntity.ok(List.of());
            }
            List<FlightOfferDto> flightOfferDtos = Arrays.stream(offers)
                    .map(offer -> FlightOfferDto.fromFlightOfferSearch(offer, finalDestinationIata))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(flightOfferDtos);
        } catch (ResponseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching flights: " + e.getMessage());
        }
    }
}