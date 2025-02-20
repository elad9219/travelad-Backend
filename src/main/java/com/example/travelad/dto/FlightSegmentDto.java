package com.example.travelad.dto;

public class FlightSegmentDto {
    private String origin;
    private String destination;
    private String departureDate;
    private String arrivalDate;
    private String duration;         // e.g., "PT45M"
    private String carrierCode;      // e.g., "RJ"
    private String flightNumber;     // e.g., "343"
    private String departureTerminal;// e.g., "3"
    private String arrivalTerminal;  // e.g., "5" or "0"
    private String aircraft;         // e.g., "E95"

    public FlightSegmentDto(String origin, String destination, String departureDate, String arrivalDate,
                            String duration, String carrierCode, String flightNumber,
                            String departureTerminal, String arrivalTerminal, String aircraft) {
        this.origin = origin;
        this.destination = destination;
        this.departureDate = departureDate;
        this.arrivalDate = arrivalDate;
        this.duration = duration;
        this.carrierCode = carrierCode;
        this.flightNumber = flightNumber;
        this.departureTerminal = departureTerminal;
        this.arrivalTerminal = arrivalTerminal;
        this.aircraft = aircraft;
    }

    // Getters and setters

    public String getOrigin() {
        return origin;
    }
    public void setOrigin(String origin) {
        this.origin = origin;
    }
    public String getDestination() {
        return destination;
    }
    public void setDestination(String destination) {
        this.destination = destination;
    }
    public String getDepartureDate() {
        return departureDate;
    }
    public void setDepartureDate(String departureDate) {
        this.departureDate = departureDate;
    }
    public String getArrivalDate() {
        return arrivalDate;
    }
    public void setArrivalDate(String arrivalDate) {
        this.arrivalDate = arrivalDate;
    }
    public String getDuration() {
        return duration;
    }
    public void setDuration(String duration) {
        this.duration = duration;
    }
    public String getCarrierCode() {
        return carrierCode;
    }
    public void setCarrierCode(String carrierCode) {
        this.carrierCode = carrierCode;
    }
    public String getFlightNumber() {
        return flightNumber;
    }
    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }
    public String getDepartureTerminal() {
        return departureTerminal;
    }
    public void setDepartureTerminal(String departureTerminal) {
        this.departureTerminal = departureTerminal;
    }
    public String getArrivalTerminal() {
        return arrivalTerminal;
    }
    public void setArrivalTerminal(String arrivalTerminal) {
        this.arrivalTerminal = arrivalTerminal;
    }
    public String getAircraft() {
        return aircraft;
    }
    public void setAircraft(String aircraft) {
        this.aircraft = aircraft;
    }
}
