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
    private Double price;
    private String imageUrl; // שדה חדש לתמונה
    private Double rating;   // שדה חדש לדירוג

    // קונסטרקטור מעודכן (המלא)
    public HotelDto(String name, String hotelId, String iataCode, String countryCode, Double latitude, Double longitude, Double price, String imageUrl, Double rating) {
        this.name = name;
        this.hotelId = hotelId;
        this.iataCode = iataCode;
        this.countryCode = countryCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.price = price;
        this.imageUrl = imageUrl;
        this.rating = rating;
    }

    // קונסטרקטור ללא תמונה ודירוג (תאימות לאחור)
    public HotelDto(String name, String hotelId, String iataCode, String countryCode, Double latitude, Double longitude, Double price) {
        this(name, hotelId, iataCode, countryCode, latitude, longitude, price, null, null);
    }

    // קונסטרקטור ישן (ללא מחיר, תמונה ודירוג)
    public HotelDto(String name, String hotelId, String iataCode, String countryCode, Double latitude, Double longitude) {
        this(name, hotelId, iataCode, countryCode, latitude, longitude, null, null, null);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHotelId() { return hotelId; }
    public void setHotelId(String hotelId) { this.hotelId = hotelId; }
    public String getIataCode() { return iataCode; }
    public void setIataCode(String iataCode) { this.iataCode = iataCode; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
}