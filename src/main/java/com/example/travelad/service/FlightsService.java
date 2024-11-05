package com.example.travelad.service;

import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.exceptions.ResponseException;
import com.amadeus.referenceData.Locations;
import com.amadeus.resources.FlightOfferSearch;
import com.amadeus.resources.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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

    public Location[] location(String keyword) throws ResponseException {
        return amadeus.referenceData.locations.get(Params
                .with("keyword", keyword)
                .and("subType", Locations.AIRPORT));
    }

    public FlightOfferSearch[] flights(String origin, String destination, String departDate, String adults, String returnDate) throws ResponseException {
        Params params = Params.with("originLocationCode", origin)
                .and("destinationLocationCode", destination)
                .and("departureDate", departDate)
                .and("adults", adults)
                .and("max", 5);  // Increase max results for more consistency

        if (returnDate != null && !returnDate.isEmpty()) {
            params.and("returnDate", returnDate);
        }

        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                // Log request details
                logger.info("Sending request to Amadeus API with parameters: originLocationCode={}, destinationLocationCode={}, departureDate={}, adults={}, returnDate={}",
                        origin, destination, departDate, adults, returnDate);

                // Make the API call and return the response if successful
                FlightOfferSearch[] flightOffers = amadeus.shopping.flightOffersSearch.get(params);

                // Log the response
                logger.info("Flight Offers Response: {}", Arrays.toString(flightOffers));

                return flightOffers;
            } catch (ResponseException e) {
                retryCount++;
                logger.error("Error fetching flights (attempt {} of {}): {}", retryCount, MAX_RETRIES, e.getMessage());

                if (retryCount >= MAX_RETRIES) {
                    throw new RuntimeException("Max retries reached. Error fetching flights: " + e.getMessage(), e);
                }

                // Exponential backoff before retrying
                try {
                    long backoff = (long) Math.pow(2, retryCount) * 1000;
                    logger.info("Retrying in {} ms...", backoff);
                    TimeUnit.MILLISECONDS.sleep(backoff);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", interruptedException);
                }
            }
        }

        return new FlightOfferSearch[0]; // Return empty array if no offers after retries
    }
}
