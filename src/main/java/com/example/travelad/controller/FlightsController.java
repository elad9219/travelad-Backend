package com.example.travelad.controller;

import com.example.travelad.dto.FlightOfferDto;
import com.example.travelad.service.CityCacheService;
import com.example.travelad.service.FlightsService;
import com.example.travelad.utils.IataCodeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/flights")
@CrossOrigin(origins = "*")
public class FlightsController {

    private final FlightsService flightsService;

    public FlightsController(FlightsService flightsService) {
        this.flightsService = flightsService;
    }

    @GetMapping
    public ResponseEntity<List<FlightOfferDto>> flights(
            @RequestParam("city") String city) {
        String destination = IataCodeUtils.getIataCodeForCity(
                CityCacheService.normalizeCity(city)
        );

        if (destination == null) {
            return ResponseEntity.ok(List.of());
        }

        LocalDate today = LocalDate.now();

        return ResponseEntity.ok(flightsService.flights(
                "TLV",
                destination,
                today.plusDays(10).toString(),
                "1",
                today.plusDays(15).toString()
        ));
    }

    @GetMapping("/advancedFlightSearch")
    public ResponseEntity<List<FlightOfferDto>> advancedFlightSearch(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam("departDate") String departDate,
            @RequestParam(
                    value = "returnDate",
                    required = false,
                    defaultValue = ""
            ) String returnDate,
            @RequestParam("adults") String adults) {
        String from = IataCodeUtils.getIataCodeForCity(
                CityCacheService.normalizeCity(origin)
        );
        String to = IataCodeUtils.getIataCodeForCity(
                CityCacheService.normalizeCity(destination)
        );

        if (from == null || to == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid origin or destination city"
            );
        }

        return ResponseEntity.ok(
                flightsService.flights(
                        from,
                        to,
                        departDate,
                        adults,
                        returnDate
                )
        );
    }
}