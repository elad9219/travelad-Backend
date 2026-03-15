package com.example.travelad.dto;

import java.util.List;

public class FlightOfferDto {

    private String id;
    private String source;

    // Changed to double to perfectly match MockFlightUtils expected format
    private double price;

    // Flat fields instead of nested Amadeus Itinerary objects
    private String outboundDuration;
    private String returnDuration;
    private List<FlightSegmentDto> segments;
    private List<FlightSegmentDto> outboundSegments;
    private List<FlightSegmentDto> returnSegments;

    private List<String> validatingAirlineCodes;
    private String airlineName;
    private String airlineLogo;

    public FlightOfferDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getOutboundDuration() { return outboundDuration; }
    public void setOutboundDuration(String outboundDuration) { this.outboundDuration = outboundDuration; }

    public String getReturnDuration() { return returnDuration; }
    public void setReturnDuration(String returnDuration) { this.returnDuration = returnDuration; }

    public List<FlightSegmentDto> getSegments() { return segments; }
    public void setSegments(List<FlightSegmentDto> segments) { this.segments = segments; }

    public List<FlightSegmentDto> getOutboundSegments() { return outboundSegments; }
    public void setOutboundSegments(List<FlightSegmentDto> outboundSegments) { this.outboundSegments = outboundSegments; }

    public List<FlightSegmentDto> getReturnSegments() { return returnSegments; }
    public void setReturnSegments(List<FlightSegmentDto> returnSegments) { this.returnSegments = returnSegments; }

    public List<String> getValidatingAirlineCodes() { return validatingAirlineCodes; }
    public void setValidatingAirlineCodes(List<String> validatingAirlineCodes) { this.validatingAirlineCodes = validatingAirlineCodes; }

    public String getAirlineName() { return airlineName; }
    public void setAirlineName(String airlineName) { this.airlineName = airlineName; }

    public String getAirlineLogo() { return airlineLogo; }
    public void setAirlineLogo(String airlineLogo) { this.airlineLogo = airlineLogo; }
}