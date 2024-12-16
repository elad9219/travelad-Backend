package com.example.travelad.dto;


public class LocationDto {
    private String name;
    private String iataCode;
    private String countryCode;
    private String countryName;
    private String cityName;
    private String cityCode;



    public LocationDto(String name, String iataCode, String countryCode, String countryName, String cityName, String cityCode) {
        this.name = name;
        this.iataCode = iataCode;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.cityName = cityName;
        this.cityCode = cityCode;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }
}

