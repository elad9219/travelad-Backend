package com.example.travelad.dto;

public class RoomDto {
    private String bedType;
    private int beds;
    private String description;

    public RoomDto(String bedType, int beds, String description) {
        this.bedType = bedType;
        this.beds = beds;
        this.description = description;
    }

    public String getBedType() {
        return bedType;
    }
    public void setBedType(String bedType) {
        this.bedType = bedType;
    }
    public int getBeds() {
        return beds;
    }
    public void setBeds(int beds) {
        this.beds = beds;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}
