package com.example.travelad.controller;

import com.example.travelad.dto.FlightOfferDto;
import com.example.travelad.service.FlightsService;
import com.example.travelad.utils.IataCodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/flights")
@CrossOrigin(origins = "*")
public class FlightsController {

    private static final Logger logger = LoggerFactory.getLogger(FlightsController.class);
    private final FlightsService flightsService;

    public FlightsController(FlightsService flightsService) {
        this.flightsService = flightsService;
    }

    @GetMapping
    public ResponseEntity<?> flights(@RequestParam String city) {
        final String destinationIata = IataCodeUtils.getIataCodeForCity(city);

        if (destinationIata == null) {
            return ResponseEntity.ok().body("No IATA code found for the specified city.");
        }

        try {
            String origin = "TLV";
            String departDate = LocalDate.now().plusDays(10).toString();
            String returnDate = LocalDate.now().plusDays(15).toString();
            String adults = "1";

            // הסרוויס מחזיר עכשיו ישר את ה-DTO, אין צורך בהמרה כאן
            List<FlightOfferDto> flightOfferDtos = flightsService.flights(origin, destinationIata, departDate, adults, returnDate);

            return ResponseEntity.ok(flightOfferDtos);

        } catch (Exception e) {
            logger.error("Error fetching flights (mock): {}", e.getMessage());
            return ResponseEntity.status(500).body("An unexpected error occurred.");
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

        try {
            List<FlightOfferDto> flightOfferDtos = flightsService.flights(originIata, destinationIata, departDate, adults, returnDate);
            return ResponseEntity.ok(flightOfferDtos);

        } catch (Exception e) {
            logger.error("Error fetching advanced flights (mock): {}", e.getMessage());
            return ResponseEntity.status(500).body("An unexpected error occurred.");
        }
    }
}