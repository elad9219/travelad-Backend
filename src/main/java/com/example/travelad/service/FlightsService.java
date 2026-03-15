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

    // חתימת הפונקציה הוחזרה בדיוק לסדר המקורי: origin, destination, departDate, adults, returnDate
    public List<FlightOfferDto> flights(String origin, String destination, String departDate, String adults, String returnDate) {
        logger.info("Generating MOCK flights from {} to {} on {}", origin, destination, departDate);

        // העברת הפרמטרים ל-Mock בדיוק בסדר שהוא מצפה לקבל אותם
        return MockFlightUtils.generateMockFlights(origin, destination, departDate, returnDate, adults);
    }
}