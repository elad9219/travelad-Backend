package com.example.travelad.beans;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "airline_cache_status")
public class AirlineCacheStatus {

    @Id
    private String iataCode;  // Primary key (normalized, e.g., "A3")

    private boolean complete;

    public AirlineCacheStatus() {}

    public AirlineCacheStatus(String iataCode, boolean complete) {
        this.iataCode = iataCode;
        this.complete = complete;
    }

    public String getIataCode() {
        return iataCode;
    }

    public void setIataCode(String iataCode) {
        this.iataCode = iataCode;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
