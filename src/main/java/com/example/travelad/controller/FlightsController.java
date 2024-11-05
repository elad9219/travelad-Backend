package com.example.travelad.controller;

import com.amadeus.resources.FlightOfferSearch;
import com.amadeus.resources.Location;
import com.amadeus.exceptions.ResponseException;
import com.example.travelad.beans.FlightSegmentDto;
import com.example.travelad.beans.LocationDto;
import com.example.travelad.beans.FlightOfferDto;
import com.example.travelad.service.FlightsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class FlightsController {

    private final FlightsService amadeusConnect;

    // Constructor injection
    public FlightsController(FlightsService amadeusConnect) {
        this.amadeusConnect = amadeusConnect;
    }

    // Endpoint to search for locations
    @GetMapping("/locations")
    public ResponseEntity<?> locations(@RequestParam(required = true) String keyword) {
        try {
            // Call the Amadeus API to get locations
            Location[] locations = amadeusConnect.location(keyword);

            // Check if there are any locations returned
            if (locations == null || locations.length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No locations found for the given keyword.");
            }

            // Convert the Location objects into LocationDto objects for easier handling
            List<LocationDto> locationDtos = Arrays.stream(locations)
                    .map(location -> new LocationDto(
                            location.getName(),
                            location.getIataCode(),
                            location.getAddress().getCountryCode(),
                            location.getAddress().getCountryName(),
                            location.getAddress().getCityName(),
                            location.getAddress().getCityCode()
                    ))
                    .collect(Collectors.toList());

            // Return the results
            return ResponseEntity.ok(locationDtos);

        } catch (ResponseException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching locations: " + e.getMessage());
        }
    }





    @GetMapping("/flights")
    public ResponseEntity<?> flights(@RequestParam String origin,
                                     @RequestParam String destination,
                                     @RequestParam String departDate,
                                     @RequestParam String adults,
                                     @RequestParam(required = false) String returnDate) {
        try {
            // Call the Amadeus API to get flight offers
            FlightOfferSearch[] flightOffers = amadeusConnect.flights(origin, destination, departDate, adults, returnDate);

            if (flightOffers == null || flightOffers.length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No flights found for the given parameters.");
            }

            List<FlightOfferDto> flightOfferDtos = Arrays.stream(flightOffers)
                    .map(offer -> {
                        List<FlightSegmentDto> segments = Arrays.stream(offer.getItineraries())
                                .flatMap(itinerary -> Arrays.stream(itinerary.getSegments()))
                                .map(segment -> new FlightSegmentDto(
                                        segment.getDeparture().getIataCode(),
                                        segment.getArrival().getIataCode(),
                                        segment.getDeparture().getAt(),
                                        segment.getArrival().getAt()))
                                .collect(Collectors.toList());

                        return new FlightOfferDto(segments, offer.getPrice().getTotal());
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(flightOfferDtos);

        } catch (ResponseException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching flights: " + e.getMessage());
        }
    }
}
