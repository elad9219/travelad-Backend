package com.example.travelad.repositories;

import com.example.travelad.beans.AirlineCacheStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AirlineCacheStatusRepository extends JpaRepository<AirlineCacheStatus, String> {
    // Primary key is iataCode
}
