package com.example.travelad.dto;

import com.amadeus.resources.FlightOfferSearch;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FlightOfferDto {
    private List<FlightSegmentDto> segments;
    private double price;

    public FlightOfferDto(List<FlightSegmentDto> segments, double price) {
        this.segments = segments;
        this.price = price;
    }

    public List<FlightSegmentDto> getSegments() {
        return segments;
    }

    public void setSegments(List<FlightSegmentDto> segments) {
        this.segments = segments;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    // Static method to convert from FlightOfferSearch to FlightOfferDto
    public static FlightOfferDto fromFlightOfferSearch(FlightOfferSearch offer) {
        List<FlightSegmentDto> segments = Arrays.stream(offer.getItineraries())
                .flatMap(itinerary -> Arrays.stream(itinerary.getSegments()))
                .map(segment -> new FlightSegmentDto(
                        segment.getDeparture().getIataCode(),
                        segment.getArrival().getIataCode(),
                        segment.getDeparture().getAt(),
                        segment.getArrival().getAt()))
                .collect(Collectors.toList());

        double totalPrice;
        try {
            totalPrice = Double.parseDouble(offer.getPrice().getTotal());
        } catch (NumberFormatException e) {
            totalPrice = 0.0;
        }

        return new FlightOfferDto(segments, totalPrice);
    }
}