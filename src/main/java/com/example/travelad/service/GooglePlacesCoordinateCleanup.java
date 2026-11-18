package com.example.travelad.service;

import com.example.travelad.repositories.GooglePlacesRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class GooglePlacesCoordinateCleanup {

    private final GooglePlacesRepository repository;

    public GooglePlacesCoordinateCleanup(GooglePlacesRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void clearExpiredCoordinates() {
        // Leave margin for scheduling delays and local clock differences.
        repository.clearExpiredCoordinates(LocalDateTime.now().minusDays(29));
    }
}