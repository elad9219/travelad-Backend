package com.example.travelad.repositories;

import com.example.travelad.beans.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repository interface for accessing Hotel data in PostgreSQL
public interface HotelRepository extends JpaRepository<Hotel, String> {
    // Custom query to find hotels by city code
    List<Hotel> findByCityCode(String cityCode);
}