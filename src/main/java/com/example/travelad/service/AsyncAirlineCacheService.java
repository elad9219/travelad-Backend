package com.example.travelad.service;

import com.example.travelad.beans.Airline;
import com.example.travelad.beans.AirlineCacheStatus;
import com.example.travelad.repositories.AirlineCacheStatusRepository;
import com.example.travelad.repositories.AirlineRepository;
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

    /**
     * Saves the airline data asynchronously in a thread-safe manner.
     * For the given IATA code, only one thread at a time can perform the update.
     * If an airline record already exists, it is updated; otherwise, a new record is inserted.
     * Then, the persistent cache status for the IATA code is updated.
     */
    @Async
    public void saveAirlineAsync(Airline airline, String iataCode) {
        // Synchronize on the interned iataCode to ensure only one thread works with it at once.
        synchronized (iataCode.intern()) {
            try {
                Optional<Airline> existingAirline = airlineRepository.findById(iataCode);
                if (existingAirline.isPresent()) {
                    Airline existing = existingAirline.get();
                    existing.setIcaoCode(airline.getIcaoCode());
                    existing.setLogoUrl(airline.getLogoUrl());
                    airlineRepository.save(existing);
                } else {
                    airlineRepository.save(airline);
                }
            } catch (DataAccessException e) {
                logger.error("Error saving airline asynchronously for IATA code {}: {}", iataCode, e.getMessage());
            }
            // Update the persistent cache status for the IATA code.
            try {
                Optional<AirlineCacheStatus> optStatus = cacheStatusRepository.findById(iataCode);
                if (optStatus.isPresent()) {
                    AirlineCacheStatus status = optStatus.get();
                    if (!status.isComplete()) {
                        status.setComplete(true);
                        cacheStatusRepository.save(status);
                        logger.info("Updated cache status for IATA code {} to true.", iataCode);
                    } else {
                        logger.info("Cache status for IATA code {} already true.", iataCode);
                    }
                } else {
                    cacheStatusRepository.save(new AirlineCacheStatus(iataCode, true));
                    logger.info("Inserted cache status for IATA code {} as true.", iataCode);
                }
            } catch (DataAccessException e) {
                logger.error("Error updating cache status for IATA code {}: {}", iataCode, e.getMessage());
            }
        }
    }
}
