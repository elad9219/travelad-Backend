package com.example.travelad.repositories;

import com.example.travelad.beans.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttractionRepository extends JpaRepository<Attraction, Long> {
    List<Attraction> findByCityIgnoreCase(String city);
    Optional<Attraction> findByNameAndCityIgnoreCase(String name, String city);
}
