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
        // Lookup the IATA code for the city using FIELD2 for the search.
        final String destinationIata = IataCodeUtils.getIataCodeForCity(city);
        if (destinationIata == null) {
            return ResponseEntity.ok(List.of());
        }
        // Lookup the final destination IATA code (FIELD3) and default to destinationIata if null.
        final String tempFinal = IataCodeUtils.getFinalDestinationIataForCity(city);
        final String finalDestinationIata = tempFinal != null ? tempFinal : destinationIata;

        try {
            String origin = "TLV"; // replace with your origin if needed
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
}
