package com.example.travelad.service;

import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.HotelOfferSearch;
import com.amadeus.resources.Hotel;
import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.beans.HotelCacheStatus;
import com.example.travelad.repositories.HotelCacheStatusRepository;
import com.example.travelad.repositories.HotelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HotelsService {

    private static final Logger logger = LoggerFactory.getLogger(HotelsService.class);
    private Amadeus amadeus;
    private final GooglePlacesService googlePlacesService;
    private final HotelRepository hotelRepository;
    private final AsyncHotelCacheService asyncHotelCacheService;
    private final HotelCacheStatusRepository cacheStatusRepository;

    @Value("${amadeus.api.key}")
    private String apiKey;

    @Value("${amadeus.api.secret}")
    private String apiSecret;

    public HotelsService(GooglePlacesService googlePlacesService,
                         HotelRepository hotelRepository,
                         AsyncHotelCacheService asyncHotelCacheService,
                         HotelCacheStatusRepository cacheStatusRepository) {
        this.googlePlacesService = googlePlacesService;
        this.hotelRepository = hotelRepository;
        this.asyncHotelCacheService = asyncHotelCacheService;
        this.cacheStatusRepository = cacheStatusRepository;
    }

    @PostConstruct
    public void init() {
        this.amadeus = Amadeus.builder(apiKey, apiSecret).build();
    }

    /**
     * Maps an Amadeus hotel (com.amadeus.resources.Hotel) to our Hotel entity.
     */
    private com.example.travelad.beans.Hotel mapToHotelEntity(Hotel apiHotel, String cityCode) {
        com.example.travelad.beans.Hotel entity = new com.example.travelad.beans.Hotel();
        entity.setHotelId(apiHotel.getHotelId());
        entity.setName(apiHotel.getName());
        entity.setCityCode(cityCode);
        if (apiHotel.getAddress() != null) {
            entity.setCountryCode(apiHotel.getAddress().getCountryCode());
        }
        if (apiHotel.getGeoCode() != null) {
            try {
                entity.setLatitude(Double.valueOf(apiHotel.getGeoCode().getLatitude()));
                entity.setLongitude(Double.valueOf(apiHotel.getGeoCode().getLongitude()));
            } catch (Exception e) {
                logger.warn("Error converting geocode for hotel {}: {}", apiHotel.getHotelId(), e.getMessage());
            }
        }
        return entity;
    }

    /**
     * Calls the API to search hotels by city name.
     */
    public Hotel[] searchHotelsByCityNameFromApi(String cityName) throws ResponseException {
        GooglePlaces place = googlePlacesService.searchPlaceByCity(cityName);
        if (place == null) {
            logger.warn("No place found for city name: {}", cityName);
            return new Hotel[0];
        }
        logger.info("Fetching hotels for city name: {} using geocode: {}, {}",
                cityName, place.getLatitude(), place.getLongitude());
        return amadeus.referenceData.locations.hotels.byGeocode.get(
                Params.with("latitude", String.valueOf(place.getLatitude()))
                        .and("longitude", String.valueOf(place.getLongitude()))
        );
    }

    /**
     * Searches hotels by city name.
     * If the persistent cache status is marked complete for the city, returns hotels from DB.
     * Otherwise, returns API results immediately and asynchronously saves hotels,
     * updating the persistent cache status in the database.
     */
    public com.example.travelad.beans.Hotel[] searchHotelsByCityName(String cityName) {
        String normalizedCity = cityName.toLowerCase();
        boolean cacheComplete = cacheStatusRepository.findById(normalizedCity)
                .map(HotelCacheStatus::isComplete)
                .orElse(false);
        if (cacheComplete) {
            List<com.example.travelad.beans.Hotel> cachedHotels = hotelRepository.findByCityCode(normalizedCity);
            if (cachedHotels != null && !cachedHotels.isEmpty()) {
                logger.info("Returning cached hotels for city: {}", normalizedCity);
                return cachedHotels.toArray(new com.example.travelad.beans.Hotel[0]);
            }
        }
        try {
            Hotel[] apiHotels = searchHotelsByCityNameFromApi(cityName);
            List<com.example.travelad.beans.Hotel> entities = Arrays.stream(apiHotels)
                    .map(apiHotel -> mapToHotelEntity(apiHotel, normalizedCity))
                    .collect(Collectors.toList());
            // Save the cache status as incomplete initially
            cacheStatusRepository.save(new HotelCacheStatus(normalizedCity, false));
            // Save hotels asynchronously; once saved, the cache status will be updated.
            asyncHotelCacheService.saveHotelsAsync(entities, normalizedCity);
            return entities.toArray(new com.example.travelad.beans.Hotel[0]);
        } catch (ResponseException e) {
            logger.error("Error fetching hotels from API: {}", e.getMessage());
            throw new RuntimeException("Error fetching hotels from API", e);
        }
    }

    /**
     * Searches hotels by city code (synchronously).
     */
    public com.example.travelad.beans.Hotel[] searchHotelsByCityCode(String cityCode) throws ResponseException {
        String normalizedCity = cityCode.toLowerCase();
        List<com.example.travelad.beans.Hotel> cachedHotels = hotelRepository.findByCityCode(normalizedCity);
        if (cachedHotels != null && !cachedHotels.isEmpty()) {
            logger.info("Returning cached hotels for city: {}", normalizedCity);
            return cachedHotels.toArray(new com.example.travelad.beans.Hotel[0]);
        }
        Hotel[] apiHotels = amadeus.referenceData.locations.hotels.byCity.get(
                Params.with("cityCode", cityCode)
        );
        List<com.example.travelad.beans.Hotel> entities = Arrays.stream(apiHotels)
                .map(apiHotel -> mapToHotelEntity(apiHotel, normalizedCity))
                .collect(Collectors.toList());
        for (com.example.travelad.beans.Hotel hotel : entities) {
            try {
                hotelRepository.save(hotel);
                logger.info("Saved hotel: {} in {}", hotel.getName(), normalizedCity);
            } catch (DataAccessException e) {
                logger.error("Error saving hotel {}: {}", hotel.getName(), e.getMessage());
            }
        }
        return entities.toArray(new com.example.travelad.beans.Hotel[0]);
    }

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
        try {
            return amadeus.shopping.hotelOffersSearch.get(params);
        } catch (Exception e) {
            logger.error("Error fetching hotel offers: {}", e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred while fetching hotel offers", e);
        }
    }
}
