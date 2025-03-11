package com.example.travelad.beans;

import javax.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "google_places")
public class GooglePlaces {

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String placeId;

    @Column(name = "city")
    private String city;

    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    @Column(length = 1000)
    private String icon;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private boolean isComplete;
    private int attractionCount;

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public boolean isComplete() { return isComplete; }
    public void setComplete(boolean complete) { this.isComplete = complete; }

    public int getAttractionCount() { return attractionCount; }
    public void setAttractionCount(int attractionCount) { this.attractionCount = attractionCount; }
}