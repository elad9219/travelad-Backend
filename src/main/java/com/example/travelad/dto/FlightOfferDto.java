package com.example.travelad.dto;

import com.amadeus.resources.FlightOfferSearch;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.travelad.utils.AirlineServiceStatic;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlightOfferDto {
    // For one-way flights:
    private List<FlightSegmentDto> segments;
    // For round-trip flights:
    private List<FlightSegmentDto> outboundSegments;
    private List<FlightSegmentDto> returnSegments;
    private double price;

    public FlightOfferDto() {
    }

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

    public static FlightOfferDto fromFlightOfferSearch(FlightOfferSearch offer, String finalDestinationIata) {
        FlightOfferDto dto = new FlightOfferDto();
        double totalPrice;
        try {
            totalPrice = Double.parseDouble(offer.getPrice().getTotal());
        } catch (NumberFormatException e) {
            totalPrice = 0.0;
        }
        dto.setPrice(totalPrice);

        // Use lambda with type inference to map each segment.
        // Note: We assume that offer.getItineraries()[].getSegments() returns an array of objects
        // that have the methods getDeparture(), getArrival(), getDuration(), etc.
        // The lambda uses these methods without explicitly referencing a Segment type.
        // Also, we enrich each segment with an airline logo URL.
        if (offer.getItineraries().length == 1) {
            List<FlightSegmentDto> segments = Arrays.stream(offer.getItineraries()[0].getSegments())
                    .map(seg -> new FlightSegmentDto(
                            seg.getDeparture().getIataCode(),
                            seg.getArrival().getIataCode(),
                            seg.getDeparture().getAt(),
                            seg.getArrival().getAt(),
                            seg.getDuration(),
                            seg.getCarrierCode(),
                            seg.getNumber(),
                            seg.getAircraft().getCode(),
                            seg.getDeparture().getTerminal(),
                            seg.getArrival().getTerminal(),
                            AirlineServiceStatic.getAirlineLogoUrl(seg.getCarrierCode())
                    ))
                    .collect(Collectors.toList());
            dto.setSegments(segments);
        } else {
            List<FlightSegmentDto> segments = Arrays.stream(offer.getItineraries())
                    .flatMap(itinerary -> Arrays.stream(itinerary.getSegments()))
                    .map(seg -> new FlightSegmentDto(
                            seg.getDeparture().getIataCode(),
                            seg.getArrival().getIataCode(),
                            seg.getDeparture().getAt(),
                            seg.getArrival().getAt(),
                            seg.getDuration(),
                            seg.getCarrierCode(),
                            seg.getNumber(),
                            seg.getAircraft().getCode(),
                            seg.getDeparture().getTerminal(),
                            seg.getArrival().getTerminal(),
                            AirlineServiceStatic.getAirlineLogoUrl(seg.getCarrierCode())
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