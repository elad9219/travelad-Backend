package com.example.travelad.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HotelDto {
    private String name;
    private String hotelId;
    private String iataCode;
    private String countryCode;
    private Double latitude;
    private Double longitude;

    public HotelDto(String name, String hotelId, String iataCode, String countryCode, Double latitude, Double longitude) {
        this.name = name;
        this.hotelId = hotelId;
        this.iataCode = iataCode;
        this.countryCode = countryCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and setters
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getHotelId() {
        return hotelId;
    }
    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }
    public String getIataCode() {
        return iataCode;
    }
    public void setIataCode(String iataCode) {
        this.iataCode = iataCode;
    }
    public String getCountryCode() {
        return countryCode;
    }
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    public Double getLatitude() {
        return latitude;
    }
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    public Double getLongitude() {
        return longitude;
    }
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
