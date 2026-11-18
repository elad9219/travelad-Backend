package com.example.travelad.repositories;

import com.example.travelad.beans.GooglePlaces;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GooglePlacesRepository extends JpaRepository<GooglePlaces, Long> {

     List<GooglePlaces> findByCityIgnoreCase(String city);

     Optional<GooglePlaces> findByPlaceId(String placeId);

     @Transactional
     @Modifying
     @Query("""
            update GooglePlaces place
            set place.latitude = null, place.longitude = null
            where (place.createdAt is null or place.createdAt <= :cutoff)
              and (place.latitude is not null or place.longitude is not null)
            """)
     int clearExpiredCoordinates(@Param("cutoff") LocalDateTime cutoff);

     @Transactional
     @Modifying
     @Query("""
            update GooglePlaces place
            set place.name = null, place.address = null, place.country = null, place.icon = null
            where place.name is not null or place.address is not null or place.country is not null
              or (place.icon is not null and place.icon <> '')
            """)
     int clearStoredDisplayDetails();
}