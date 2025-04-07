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

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 2000;

    @PostConstruct
    public void init() {
        this.amadeus = Amadeus.builder(apiKey, apiSecret).build();
    }

    public FlightOfferSearch[] flights(String origin, String destination, String departDate, String adults, String returnDate) throws ResponseException {
        Params params = Params.with("originLocationCode", origin)
                .and("destinationLocationCode", destination)
                .and("departureDate", departDate)
                .and("adults", adults)
                .and("max", 15);

        if (returnDate != null && !returnDate.isEmpty()) {
            params.and("returnDate", returnDate);
        }

        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                logger.info("Starting flight search with params: {}", params);
                FlightOfferSearch[] offers = amadeus.shopping.flightOffersSearch.get(params);
                logger.info("Successfully fetched {} flight offers", offers.length);
                return offers;
            } catch (ResponseException e) {
                retryCount++;
                logger.error("Error fetching flights: {}. Retrying ({}/{})", e.getMessage(), retryCount, MAX_RETRIES);
                if (retryCount >= MAX_RETRIES) {
                    logger.error("Max retries reached. Throwing exception: {}", e.getMessage());
                    throw e;
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ResponseException(e.getResponse());
                }
            }
        }
        logger.warn("No flight offers returned after retries.");
        return new FlightOfferSearch[0];
    }
}