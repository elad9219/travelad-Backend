package com.example.travelad.repositories;

import com.example.travelad.beans.HotelCacheStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelCacheStatusRepository extends JpaRepository<HotelCacheStatus, String> {
    // The primary key is "city" (normalized to lower-case)
}
