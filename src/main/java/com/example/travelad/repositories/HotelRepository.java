package com.example.travelad.repositories;

import com.example.travelad.beans.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, String> {
    // Retrieve hotels by city code (stored in lower-case)
    List<Hotel> findByCityCode(String cityCode);
}
