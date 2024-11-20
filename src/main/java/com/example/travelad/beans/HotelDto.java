package com.example.travelad.beans;

public class HotelDto {
    private String name;
    private String city;
    private String country;
    private String price; // Changed to String to align with Amadeus 10.0.0
    private String vendor;

    public HotelDto(String name, String city, String country, String price, String vendor) {
        this.name = name;
        this.city = city;
        this.country = country;
        this.price = price;
        this.vendor = vendor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
}

