package com.example.travelad.service;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.dto.HotelDto;
import com.example.travelad.utils.MockDataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class HotelsService {

    private static final Logger logger = LoggerFactory.getLogger(HotelsService.class);
    private final GooglePlacesService googlePlacesService;

    public HotelsService(GooglePlacesService googlePlacesService) {
        this.googlePlacesService = googlePlacesService;
    }

    public List<HotelDto> searchHotelsByCityName(String cityName) {
        String decodedCity = cityName;
        try {
            decodedCity = URLDecoder.decode(cityName, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Failed to decode city name: {}", cityName);
        }

        logger.info("Searching hotels (MOCK) for city: {}", decodedCity);

        // Get coordinates for the city
        GooglePlaces place = googlePlacesService.searchPlaceByCity(decodedCity);

        double lat = 0.0;
        double lon = 0.0;

        if (place != null && Math.abs(place.getLatitude()) > 0.001 && Math.abs(place.getLongitude()) > 0.001) {
            lat = place.getLatitude();
            lon = place.getLongitude();
        } else {
            logger.warn("City coordinates not found for {}, using 0,0 fallback.", decodedCity);
        }

        // generateMockHotels creates hotels with a 'price' field.
        // We will treat this price as 'Price Per Night' in the Frontend.
        return MockDataUtils.generateMockHotels(decodedCity, lat, lon);
    }

    public Object searchHotelOffers(String hotelIds, String checkInDate, String checkOutDate, Integer adults) {
        return List.of();
    }
}