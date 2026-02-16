package com.example.travelad.service;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.dto.HotelDto;
import com.example.travelad.utils.MockDataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class HotelsService {

    private static final Logger logger = LoggerFactory.getLogger(HotelsService.class);
    private final GooglePlacesService googlePlacesService;

    public HotelsService(GooglePlacesService googlePlacesService) {
        this.googlePlacesService = googlePlacesService;
    }

    public List<HotelDto> searchHotelsByCityName(String cityName, String checkInDate, String checkOutDate, Integer adults) {
        String decodedCity = cityName;
        try {
            decodedCity = URLDecoder.decode(cityName, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Failed to decode city name: {}", cityName);
        }

        int numAdults = (adults != null && adults > 0) ? adults : 1;
        logger.info("Searching hotels for city: {} | Dates: {} to {} | Guests: {}", decodedCity, checkInDate, checkOutDate, numAdults);

        // חישוב מספר לילות
        long nights = 1;
        try {
            if (checkInDate != null && checkOutDate != null) {
                LocalDate start = LocalDate.parse(checkInDate);
                LocalDate end = LocalDate.parse(checkOutDate);
                nights = ChronoUnit.DAYS.between(start, end);
                if (nights <= 0) nights = 1;
            }
        } catch (Exception e) {
            logger.warn("Date parsing failed, defaulting to 1 night");
        }

        // קבלת קואורדינטות מגוגל
        GooglePlaces place = googlePlacesService.searchPlaceByCity(decodedCity);
        double lat = 0.0;
        double lon = 0.0;

        if (place != null && Math.abs(place.getLatitude()) > 0.001 && Math.abs(place.getLongitude()) > 0.001) {
            lat = place.getLatitude();
            lon = place.getLongitude();
        }

        // יצירת נתוני דמה
        List<HotelDto> hotels = MockDataUtils.generateMockHotels(decodedCity, lat, lon);

        // חישוב מחיר: (מחיר ללילה * לילות) + (20% תוספת על כל אורח נוסף)
        double guestMultiplier = 1 + ((numAdults - 1) * 0.20);

        for (HotelDto hotel : hotels) {
            if (hotel.getPrice() != null) {
                double basePriceForNights = hotel.getPrice() * nights;
                hotel.setPrice(basePriceForNights * guestMultiplier);
            }
        }

        return hotels;
    }

    public Object searchHotelOffers(String hotelIds, String checkInDate, String checkOutDate, Integer adults) {
        return List.of();
    }
}