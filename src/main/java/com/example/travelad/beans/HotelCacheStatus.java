package com.example.travelad.beans;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "hotel_cache_status")
public class HotelCacheStatus {

    @Id
    private String city; // e.g., "paris", "tel aviv" (normalized to lower-case)

    private boolean complete;

    public HotelCacheStatus() {}

    public HotelCacheStatus(String city, boolean complete) {
        this.city = city;
        this.complete = complete;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
