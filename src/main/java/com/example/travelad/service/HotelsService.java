package com.example.travelad.service;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.dto.HotelDto;
import com.example.travelad.utils.MockDataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotelsService {

    private static final Logger logger = LoggerFactory.getLogger(HotelsService.class);
    private final GooglePlacesService googlePlacesService;

    // הסרנו את התלויות ב-Repositories וב-Amadeus עבור המוק
    public HotelsService(GooglePlacesService googlePlacesService) {
        this.googlePlacesService = googlePlacesService;
    }

    // שינינו את החתימה כדי שתחזיר ישר List<HotelDto>
    public List<HotelDto> searchHotelsByCityName(String cityName) {
        logger.info("Searching hotels (MOCK) for city: {}", cityName);

        // 1. קבלת קואורדינטות מגוגל כדי שהמלונות יהיו במקום הנכון
        GooglePlaces place = googlePlacesService.searchPlaceByCity(cityName);
        double lat = 0.0;
        double lon = 0.0;

        if (place != null) {
            lat = place.getLatitude();
            lon = place.getLongitude();
        }

        // 2. יצירת נתוני דמה
        return MockDataUtils.generateMockHotels(cityName, lat, lon);
    }

    // פונקציה זו יכולה להישאר ריקה או להחזיר מוק אם תרצה שהחיפוש המתקדם גם יעבוד
    // אבל בגרסה הזו המלונות מגיעים עם מחיר כבר בפונקציה הראשונה.
    public Object searchHotelOffers(String hotelIds, String checkInDate, String checkOutDate, Integer adults) {
        return List.of();
    }
}