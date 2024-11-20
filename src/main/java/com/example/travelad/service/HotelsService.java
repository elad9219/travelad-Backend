package com.example.travelad.service;

import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.Hotel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class HotelsService {

    private static final Logger logger = LoggerFactory.getLogger(HotelsService.class);

    private Amadeus amadeus;

    @Value("${amadeus.api.key}")
    private String apiKey;

    @Value("${amadeus.api.secret}")
    private String apiSecret;

    @PostConstruct
    public void init() {
        this.amadeus = Amadeus.builder(apiKey, apiSecret).build();
    }

    public Hotel[] searchHotelsByCity(String cityCode) throws ResponseException {
        try {
            logger.info("Fetching hotels for cityCode: {}", cityCode);

            return amadeus.referenceData.locations.hotels.byCity.get(
                    Params.with("cityCode", cityCode)
            );

        } catch (ResponseException e) {
            logger.error("Error fetching hotels by city: {}", e.getMessage());
            throw e;
        }
    }
}