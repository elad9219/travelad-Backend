package com.example.travelad.beans;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

// Entity representing an airline in the PostgreSQL database
@Entity
@Table(name = "airlines")
public class Airline {
    @Id
    private String iataCode;
    private String icaoCode;
    private String logoUrl;

    public Airline() {}

    public String getIataCode() {
        return iataCode;
    }

    public void setIataCode(String iataCode) {
        this.iataCode = iataCode;
    }

    public String getIcaoCode() {
        return icaoCode;
    }

    public void setIcaoCode(String icaoCode) {
        this.icaoCode = icaoCode;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}