package com.example.travelad.service;

import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.FlightOfferSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class FlightsService {

    private static final Logger logger = LoggerFactory.getLogger(FlightsService.class);
    private Amadeus amadeus;

    @Value("${amadeus.api.key}")
    private String apiKey;

    @Value("${amadeus.api.secret}")
    private String apiSecret;

    private static final int MAX_RETRIES = 3;

    @PostConstruct
    public void init() {
        this.amadeus = Amadeus.builder(apiKey, apiSecret).build();
    }

    public FlightOfferSearch[] flights(String origin, String destination, String departDate, String adults, String returnDate) throws ResponseException {
        Params params = Params.with("originLocationCode", origin)
                .and("destinationLocationCode", destination)
                .and("departureDate", departDate)
                .and("returnDate", returnDate)
                .and("adults", adults)
                .and("max", 5);

        if (returnDate != null && !returnDate.isEmpty()) {
            params.and("returnDate", returnDate);
        }

        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                logger.info("Requesting flight offers with params: {}", params);
                return amadeus.shopping.flightOffersSearch.get(params);
            } catch (ResponseException e) {
                retryCount++;
                logger.error("Error fetching flights: {}, retrying ({}/{})", e.getMessage(), retryCount, MAX_RETRIES);
                if (retryCount >= MAX_RETRIES) throw e;
            }
        }
        return new FlightOfferSearch[0];
    }
}