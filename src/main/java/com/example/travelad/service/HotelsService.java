package com.example.travelad.service;

import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.HotelOfferSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.amadeus.resources.Hotel;


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

    // Method to search hotels by city code
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

    // Modify the method to handle optional parameters
    public HotelOfferSearch[] searchHotelOffers(String hotelIds, String checkInDate, String checkOutDate, Integer adults) throws ResponseException {
        Params params = Params.with("hotelIds", hotelIds);

        if (checkInDate != null && !checkInDate.isEmpty()) {
            params.and("checkInDate", checkInDate);
        }
        if (checkOutDate != null && !checkOutDate.isEmpty()) {
            params.and("checkOutDate", checkOutDate);
        }
        if (adults != null) {
            params.and("adults", adults);
        }

        logger.info("Fetching hotel offers for hotelIds: {}, checkInDate: {}, checkOutDate: {}, adults: {}",
                hotelIds, checkInDate, checkOutDate, adults);
        return amadeus.shopping.hotelOffersSearch.get(params);
    }
}

