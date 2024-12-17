package com.example.travelad.repositories;

import com.example.travelad.beans.GooglePlaces;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GooglePlacesRepository extends JpaRepository<GooglePlaces, Long> {
     List<GooglePlaces> findByCityIgnoreCase(String city);
     Optional<GooglePlaces> findByPlaceId(String placeId);
}
