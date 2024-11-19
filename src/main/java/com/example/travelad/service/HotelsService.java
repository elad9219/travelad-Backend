package com.example.travelad.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class HotelsService {

    private final String apiUrl = "https://test.api.amadeus.com/v2/shopping/hotels/by-destination";
    private final String apiKey;  // Store your API key here

    private final RestTemplate restTemplate;

    public HotelsService(@Value("${amadeus.api.key}") String apiKey, RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
    }

    // Search hotels by city code
    public String searchHotelsByCity(String cityCode, Integer radius, String radiusUnit,
                                     String chainCodes, String amenities, String ratings) {

        // Build the API request URL
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("cityCode", cityCode)
                .queryParam("apikey", apiKey);

        if (radius != null) uriBuilder.queryParam("radius", radius);
        if (radiusUnit != null) uriBuilder.queryParam("radiusUnit", radiusUnit);
        if (chainCodes != null) uriBuilder.queryParam("chainCodes", chainCodes);
        if (amenities != null) uriBuilder.queryParam("amenities", amenities);
        if (ratings != null) uriBuilder.queryParam("ratings", ratings);

        // Make the GET request and get the response as a string
        ResponseEntity<String> response = restTemplate.getForEntity(uriBuilder.toUriString(), String.class);

        return response.getBody();  // Return the raw JSON response
    }
}
