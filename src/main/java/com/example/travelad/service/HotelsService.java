package com.example.travelad.service;

// HotelsConnect.java
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class HotelsService {

    private final String API_URL = "https://api.makcorps.com/citysearch";
    private final String API_KEY = "YOUR_API_KEY";

    public ResponseEntity<?> searchHotels(String cityName, int page, String currency, int numOfRooms, int numOfAdults, String checkInDate, String checkOutDate) {
        String url = String.format("%s/%s/%d/%s/%d/%d/%s/%s?api_key=%s", API_URL, cityName, page, currency, numOfRooms, numOfAdults, checkInDate, checkOutDate, API_KEY);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<?> response = restTemplate.getForEntity(url, Object.class);

        return response;
    }
}

