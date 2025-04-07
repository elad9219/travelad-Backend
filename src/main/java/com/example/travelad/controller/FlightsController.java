package com.example.travelad.controller;

import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.FlightOfferSearch;
import com.example.travelad.dto.FlightOfferDto;
import com.example.travelad.service.FlightsService;
import com.example.travelad.utils.IataCodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
            logger.warn("No IATA code found for city: {}", city);
            return ResponseEntity.ok().body("No IATA code found for the specified city.");
        }
        final String finalDestinationIata = IataCodeUtils.getFinalDestinationIataForCity(city) != null
                ? IataCodeUtils.getFinalDestinationIataForCity(city)
                : destinationIata;
        try {
            String origin = "TLV";
            String departDate = LocalDate.now().plusDays(10).toString();
            String returnDate = LocalDate.now().plusDays(15).toString();
            String adults = "1";
            logger.info("Fetching flights from {} to {} on {} with return on {}", origin, destinationIata, departDate, returnDate);
            FlightOfferSearch[] offers = flightsService.flights(origin, destinationIata, departDate, adults, returnDate);
            if (offers == null || offers.length == 0) {
                logger.info("No flight offers found for city: {}", city);
                return ResponseEntity.ok().body("No flights found for the given parameters.");
            }
            logger.info("Processing {} flight offers", offers.length);
            List<FlightOfferDto> flightOfferDtos = Arrays.stream(offers)
                    .map(offer -> FlightOfferDto.fromFlightOfferSearch(offer, finalDestinationIata))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(flightOfferDtos);
        } catch (ResponseException e) {
            logger.error("Amadeus API error fetching flights for city {}: {}", city, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Amadeus API is currently unavailable for flights. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error fetching flights for city {}: {}", city, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while fetching flights.");
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
            logger.warn("Invalid origin ({}) or destination ({}) provided", origin, destination);
            return ResponseEntity.badRequest().body("Invalid origin or destination city provided.");
        }
        final String finalDestinationIata = IataCodeUtils.getFinalDestinationIataForCity(destination) != null
                ? IataCodeUtils.getFinalDestinationIataForCity(destination)
                : destinationIata;
        try {
            logger.info("Advanced search: from {} to {} on {} with return on {}", originIata, destinationIata, departDate, returnDate);
            FlightOfferSearch[] offers = flightsService.flights(originIata, destinationIata, departDate, adults, returnDate);
            if (offers == null || offers.length == 0) {
                logger.info("No flight offers found for advanced search");
                return ResponseEntity.ok().body("No flights found for the given parameters.");
            }
            logger.info("Processing {} flight offers", offers.length);
            List<FlightOfferDto> flightOfferDtos = Arrays.stream(offers)
                    .map(offer -> FlightOfferDto.fromFlightOfferSearch(offer, finalDestinationIata))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(flightOfferDtos);
        } catch (ResponseException e) {
            logger.error("Amadeus API error in advanced flight search: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Amadeus API is currently unavailable for flights. Please try again later.");
        } catch (Exception e) {
            logger.error("Unexpected error in advanced flight search: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while fetching flights.");
        }
    }
}