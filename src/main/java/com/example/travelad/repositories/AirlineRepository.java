package com.example.travelad.repositories;

import com.example.travelad.beans.Airline;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository interface for accessing Airline data in PostgreSQL
public interface AirlineRepository extends JpaRepository<Airline, String> {
}