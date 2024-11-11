package com.example.travelad.beans;

// HotelDto.java
import java.util.List;

public class HotelDto {
    private String hotelName;
    private String hotelId;
    private List<VendorDto> vendors;

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getHotelId() {
        return hotelId;
    }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public List<VendorDto> getVendors() {
        return vendors;
    }

    public void setVendors(List<VendorDto> vendors) {
        this.vendors = vendors;
    }
}

