package com.example.travelad.dto;

import com.amadeus.resources.FlightOfferSearch;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlightOfferDto {
    // For one-way flights: only this field is set.
    private List<FlightSegmentDto> segments;
    // For round-trip flights: these fields are set.
    private List<FlightSegmentDto> outboundSegments;
    private List<FlightSegmentDto> returnSegments;
    private double price;

    // New fields for total duration of each itinerary
    private String outboundDuration;
    private String returnDuration;

    public FlightOfferDto() {
    }

    // Getters and setters

    public List<FlightSegmentDto> getSegments() {
        return segments;
    }
    public void setSegments(List<FlightSegmentDto> segments) {
        this.segments = segments;
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
    public String getOutboundDuration() {
        return outboundDuration;
    }
    public void setOutboundDuration(String outboundDuration) {
        this.outboundDuration = outboundDuration;
    }
    public String getReturnDuration() {
        return returnDuration;
    }
    public void setReturnDuration(String returnDuration) {
        this.returnDuration = returnDuration;
    }

    /**
     * Converts a FlightOfferSearch object into a FlightOfferDto.
     * It maps the segments and additional details: duration, carrier code, flight number,
     * terminals, and aircraft. If two itineraries are present, it sets outboundDuration and returnDuration.
     *
     * @param offer                The flight offer from Amadeus.
     * @param finalDestinationIata The final destination IATA code (used for splitting).
     * @return A FlightOfferDto instance.
     */
    public static FlightOfferDto fromFlightOfferSearch(FlightOfferSearch offer, String finalDestinationIata) {
        FlightOfferDto dto = new FlightOfferDto();
        double totalPrice;
        try {
            totalPrice = Double.parseDouble(offer.getPrice().getTotal());
        } catch (NumberFormatException e) {
            totalPrice = 0.0;
        }
        dto.setPrice(totalPrice);

        // If exactly 2 itineraries exist, set overall durations.
        if (offer.getItineraries().length == 2) {
            dto.setOutboundDuration(offer.getItineraries()[0].getDuration());
            dto.setReturnDuration(offer.getItineraries()[1].getDuration());
        }

        // Create a mapper for each segment
        // We'll map each segment's additional fields:
        // departure.iataCode, arrival.iataCode, departure.at, arrival.at,
        // segment.getDuration(), segment.getCarrierCode(), segment.getNumber(),
        // departure.terminal, arrival.terminal, aircraft.code.
        if (offer.getItineraries().length == 1) {
            List<FlightSegmentDto> segments = Arrays.stream(offer.getItineraries()[0].getSegments())
                    .map(segment -> new FlightSegmentDto(
                            segment.getDeparture().getIataCode(),
                            segment.getArrival().getIataCode(),
                            segment.getDeparture().getAt(),
                            segment.getArrival().getAt(),
                            segment.getDuration(),
                            segment.getCarrierCode(),
                            segment.getNumber(),
                            segment.getDeparture().getTerminal(),   // may be null if not available
                            segment.getArrival().getTerminal(),     // may be null
                            segment.getAircraft().getCode()
                    ))
                    .collect(Collectors.toList());
            dto.setSegments(segments);
        } else {
            // For round-trip, flatten segments from all itineraries.
            List<FlightSegmentDto> segments = Arrays.stream(offer.getItineraries())
                    .flatMap(itinerary -> Arrays.stream(itinerary.getSegments()))
                    .map(segment -> new FlightSegmentDto(
                            segment.getDeparture().getIataCode(),
                            segment.getArrival().getIataCode(),
                            segment.getDeparture().getAt(),
                            segment.getArrival().getAt(),
                            segment.getDuration(),
                            segment.getCarrierCode(),
                            segment.getNumber(),
                            segment.getDeparture().getTerminal(),
                            segment.getArrival().getTerminal(),
                            segment.getAircraft().getCode()
                    ))
                    .collect(Collectors.toList());
            if (segments.size() == 2) {
                dto.setOutboundSegments(segments.subList(0, 1));
                dto.setReturnSegments(segments.subList(1, 2));
            } else if (segments.size() > 2) {
                int boundaryIndex = -1;
                for (int i = 0; i < segments.size(); i++) {
                    if (segments.get(i).getDestination().equalsIgnoreCase(finalDestinationIata)) {
                        boundaryIndex = i;
                        break;
                    }
                }
                if (boundaryIndex != -1 && boundaryIndex < segments.size() - 1) {
                    dto.setOutboundSegments(segments.subList(0, boundaryIndex + 1));
                    dto.setReturnSegments(segments.subList(boundaryIndex + 1, segments.size()));
                } else {
                    int mid = segments.size() / 2;
                    dto.setOutboundSegments(segments.subList(0, mid));
                    dto.setReturnSegments(segments.subList(mid, segments.size()));
                }
            }
        }
        return dto;
    }
}
