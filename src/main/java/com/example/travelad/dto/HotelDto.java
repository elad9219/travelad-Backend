package com.example.travelad.dto;

public class HotelDto {
    private String name;
    private String hotelId;
    private String iataCode;
    private String countryCode;

    public HotelDto(String name, String hotelId, String iataCode, String countryCode) {
        this.name = name;
        this.hotelId = hotelId;
        this.iataCode = iataCode;
        this.countryCode = countryCode;
    }

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
}









