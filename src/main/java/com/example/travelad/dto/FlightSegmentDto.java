package com.example.travelad.dto;

public class FlightSegmentDto {
    private String origin;
    private String destination;
    private String departureDate;
    private String arrivalDate;
    private String duration;
    private String carrierCode;
    private String flightNumber;
    private String aircraft;
    private String aircraftFullName; // New field for full aircraft name
    private String departureTerminal;
    private String arrivalTerminal;
    private String airlineLogoUrl; // Existing field for the logo URL

    public FlightSegmentDto(String origin, String destination, String departureDate, String arrivalDate,
                            String duration, String carrierCode, String flightNumber, String aircraft,
                            String aircraftFullName, String departureTerminal, String arrivalTerminal, String airlineLogoUrl) {
        this.origin = origin;
        this.destination = destination;
        this.departureDate = departureDate;
        this.arrivalDate = arrivalDate;
        this.duration = duration;
        this.carrierCode = carrierCode;
        this.flightNumber = flightNumber;
        this.aircraft = aircraft;
        this.aircraftFullName = aircraftFullName; // Initialize new field
        this.departureTerminal = departureTerminal;
        this.arrivalTerminal = arrivalTerminal;
        this.airlineLogoUrl = airlineLogoUrl;
    }

    // Getters and Setters

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

    public String getAircraft() {
        return aircraft;
    }

    public void setAircraft(String aircraft) {
        this.aircraft = aircraft;
    }

    public String getAircraftFullName() {
        return aircraftFullName;
    }

    public void setAircraftFullName(String aircraftFullName) {
        this.aircraftFullName = aircraftFullName;
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

    public String getAirlineLogoUrl() {
        return airlineLogoUrl;
    }

    public void setAirlineLogoUrl(String airlineLogoUrl) {
        this.airlineLogoUrl = airlineLogoUrl;
    }
}