package com.example.travelad.beans;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "attraction_cache_status")
public class AttractionCacheStatus {

    @Id
    private String city;

    private Integer version;
    private Long refreshedAt;
    private boolean complete;

    public AttractionCacheStatus() {}

    public AttractionCacheStatus(String city, boolean complete) {
        this.city = city;
        this.complete = complete;
    }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Long getRefreshedAt() { return refreshedAt; }
    public void setRefreshedAt(Long refreshedAt) { this.refreshedAt = refreshedAt; }

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