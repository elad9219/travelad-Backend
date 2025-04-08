package com.example.travelad.repositories;

import com.example.travelad.beans.AttractionCacheStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttractionCacheStatusRepository extends JpaRepository<AttractionCacheStatus, String> {
}
