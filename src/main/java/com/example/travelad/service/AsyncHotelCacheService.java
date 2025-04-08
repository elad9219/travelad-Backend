package com.example.travelad.service;

import com.example.travelad.beans.Hotel;
import com.example.travelad.beans.HotelCacheStatus;
import com.example.travelad.repositories.HotelCacheStatusRepository;
import com.example.travelad.repositories.HotelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AsyncHotelCacheService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncHotelCacheService.class);
    private final HotelRepository hotelRepository;
    private final HotelCacheStatusRepository cacheStatusRepository;

    public AsyncHotelCacheService(HotelRepository hotelRepository,
                                  HotelCacheStatusRepository cacheStatusRepository) {
        this.hotelRepository = hotelRepository;
        this.cacheStatusRepository = cacheStatusRepository;
    }

    /**
     * Saves the list of hotels asynchronously.
     * If all hotels are saved successfully, update the persistent cache status in the database.
     */
    @Async
    public void saveHotelsAsync(List<Hotel> hotels, String city) {
        boolean allSaved = true;
        if (hotels != null) {
            for (Hotel hotel : hotels) {
                try {
                    hotelRepository.save(hotel);
                    logger.info("Saved hotel asynchronously: {} in {}", hotel.getName(), hotel.getCityCode());
                } catch (DataAccessException e) {
                    allSaved = false;
                    logger.error("Error saving hotel {} asynchronously: {}", hotel.getName(), e.getMessage());
                }
            }
        }
        try {
            if (allSaved) {
                cacheStatusRepository.save(new HotelCacheStatus(city, true));
                logger.info("Cache for city {} marked as complete.", city);
            } else {
                logger.warn("Cache for city {} remains incomplete.", city);
            }
        } catch (Exception e) {
            logger.error("Error updating cache status for city {}: {}", city, e.getMessage());
        }
    }
}
