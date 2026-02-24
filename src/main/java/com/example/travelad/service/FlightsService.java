package com.example.travelad.service;

import com.example.travelad.dto.FlightOfferDto;
import com.example.travelad.utils.MockFlightUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlightsService {

    private static final Logger logger = LoggerFactory.getLogger(FlightsService.class);

    // הסרנו את התלות ב-Amadeus וב-PostConstruct

    public List<FlightOfferDto> flights(String origin, String destination, String departDate, String adults, String returnDate) {
        logger.info("Generating MOCK flights from {} to {} on {}", origin, destination, departDate);

        // קריאה לפונקציה שיצרנו ב-Utils
        return MockFlightUtils.generateMockFlights(origin, destination, departDate, returnDate, adults);
    }
}