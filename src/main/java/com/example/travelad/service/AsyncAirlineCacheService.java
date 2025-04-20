package com.example.travelad.service;

import com.example.travelad.beans.Airline;
import com.example.travelad.beans.AirlineCacheStatus;
import com.example.travelad.repositories.AirlineCacheStatusRepository;
import com.example.travelad.repositories.AirlineRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AsyncAirlineCacheService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncAirlineCacheService.class);
    private final AirlineRepository airlineRepository;
    private final AirlineCacheStatusRepository cacheStatusRepository;

    public AsyncAirlineCacheService(AirlineRepository airlineRepository,
                                    AirlineCacheStatusRepository cacheStatusRepository) {
        this.airlineRepository = airlineRepository;
        this.cacheStatusRepository = cacheStatusRepository;
    }

    @Async
    public void saveAirlineAsync(Airline airline, String iataCode) {
        // Synchronize on the interned string to prevent concurrent updates for the same key.
        synchronized (iataCode.intern()) {
            try {
                Optional<Airline> optExisting = airlineRepository.findById(iataCode);
                if (optExisting.isPresent()) {
                    Airline existing = optExisting.get();
                    existing.setIcaoCode(airline.getIcaoCode());
                    existing.setLogoUrl(airline.getLogoUrl());
                    airlineRepository.save(existing);
                    logger.info("Updated airline for IATA code {}", iataCode);
                } else {
                    airlineRepository.save(airline);
                    logger.info("Saved new airline for IATA code {} with logo {}", iataCode, airline.getLogoUrl());
                }
            } catch (DataAccessException e) {
                if (e.getCause() instanceof ConstraintViolationException) {
                    // Duplicate key occurred – זה מובן, לכן נתעד כהתרעה ולא נזרוק חריגה.
                    logger.warn("Duplicate record for airline {}. Skipping insert/update.", iataCode);
                } else {
                    logger.error("Error saving airline asynchronously for IATA code {}: {}", iataCode, e.getMessage());
                }
            }

            // Update the persistent cache status for the IATA code.
            try {
                Optional<AirlineCacheStatus> optStatus = cacheStatusRepository.findById(iataCode);
                if (optStatus.isPresent()) {
                    AirlineCacheStatus status = optStatus.get();
                    if (!status.isComplete()) {
                        status.setComplete(true);
                        cacheStatusRepository.save(status);
                        logger.info("Updated cache status for IATA code {} to complete.", iataCode);
                    } else {
                        logger.info("Cache status for IATA code {} already complete.", iataCode);
                    }
                } else {
                    cacheStatusRepository.save(new AirlineCacheStatus(iataCode, true));
                    logger.info("Inserted cache status for IATA code {} as complete.", iataCode);
                }
            } catch (DataAccessException e) {
                if (e.getCause() instanceof ConstraintViolationException) {
                    logger.warn("Duplicate cache status for airline {}. Skipping cache update.", iataCode);
                } else {
                    logger.error("Error updating cache status for IATA code {}: {}", iataCode, e.getMessage());
                }
            }
        }
    }
}
