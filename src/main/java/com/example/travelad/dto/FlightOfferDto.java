package com.example.travelad.dto;

import java.util.List;

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
}