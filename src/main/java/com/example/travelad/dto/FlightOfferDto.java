package com.example.travelad.dto;

import com.amadeus.resources.FlightOfferSearch;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FlightOfferDto {
    private List<FlightSegmentDto> outboundSegments;
    private List<FlightSegmentDto> returnSegments;
    private double price;

    public FlightOfferDto(List<FlightSegmentDto> outboundSegments, List<FlightSegmentDto> returnSegments, double price) {
        this.outboundSegments = outboundSegments;
        this.returnSegments = returnSegments;
        this.price = price;
    }

    public List<FlightSegmentDto> getOutboundSegments() {
        return outboundSegments;
    }

    public void setOutboundSegments(List<FlightSegmentDto> outboundSegments) {
        this.outboundSegments = outboundSegments;
    }

    public List<FlightSegmentDto> getReturnSegments() {
        return returnSegments;
    }

    public void setReturnSegments(List<FlightSegmentDto> returnSegments) {
        this.returnSegments = returnSegments;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Converts an Amadeus FlightOfferSearch object into a FlightOfferDto.
     * The method uses the final destination IATA (FIELD3) to split the segments.
     *
     * @param offer              The flight offer from Amadeus.
     * @param finalDestinationIata The final destination IATA code (from FIELD3).
     * @return A FlightOfferDto with outbound and return segments separated.
     */
    public static FlightOfferDto fromFlightOfferSearch(FlightOfferSearch offer, String finalDestinationIata) {
        // Flatten all segments from all itineraries into a list.
        List<FlightSegmentDto> segments = Arrays.stream(offer.getItineraries())
                .flatMap(itinerary -> Arrays.stream(itinerary.getSegments()))
                .map(segment -> new FlightSegmentDto(
                        segment.getDeparture().getIataCode(),
                        segment.getArrival().getIataCode(),
                        segment.getDeparture().getAt(),
                        segment.getArrival().getAt()))
                .collect(Collectors.toList());

        List<FlightSegmentDto> outbound;
        List<FlightSegmentDto> returnSegments;
        if (segments.size() == 2) {
            // For exactly 2 segments: first is outbound, second is return.
            outbound = segments.subList(0, 1);
            returnSegments = segments.subList(1, 2);
        } else if (segments.size() > 2) {
            int boundaryIndex = -1;
            // Look for the first segment whose destination equals the final destination code.
            for (int i = 0; i < segments.size(); i++) {
                if (segments.get(i).getDestination().equalsIgnoreCase(finalDestinationIata)) {
                    boundaryIndex = i;
                    break;
                }
            }
            if (boundaryIndex != -1 && boundaryIndex < segments.size() - 1) {
                outbound = segments.subList(0, boundaryIndex + 1);
                returnSegments = segments.subList(boundaryIndex + 1, segments.size());
            } else {
                // Fallback: split the list in half.
                int mid = segments.size() / 2;
                outbound = segments.subList(0, mid);
                returnSegments = segments.subList(mid, segments.size());
            }
        } else {
            outbound = segments;
            returnSegments = List.of();
        }

        double totalPrice;
        try {
            totalPrice = Double.parseDouble(offer.getPrice().getTotal());
        } catch (NumberFormatException e) {
            totalPrice = 0.0;
        }
        return new FlightOfferDto(outbound, returnSegments, totalPrice);
    }
}
