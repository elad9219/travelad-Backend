package com.example.travelad.service;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.dto.HotelDto;
import com.example.travelad.utils.HotelCatalog;
import com.example.travelad.utils.MockHotelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class HotelsService {

    private static final Logger logger =
            LoggerFactory.getLogger(HotelsService.class);

    private final GooglePlacesService googlePlacesService;

    public HotelsService(GooglePlacesService googlePlacesService) {
        this.googlePlacesService = googlePlacesService;
    }

    public List<HotelDto> searchHotelsByCityName(
            String cityName,
            String checkInDate,
            String checkOutDate,
            Integer adults) {

        String city = CityCacheService.normalizeCity(cityName);
        int numAdults = adults == null ? 1 : adults;

        if (numAdults < 1 || numAdults > 9) {
            throw invalid("Guests must be between 1 and 9");
        }

        LocalDate today = LocalDate.now();

        LocalDate checkIn = checkInDate == null
                ? today
                : date(checkInDate);

        LocalDate checkOut = checkOutDate == null
                ? checkIn.plusDays(1)
                : date(checkOutDate);

        if (checkIn.isBefore(today)) {
            throw invalid("Check-in date must not be in the past");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw invalid("Check-out date must be after check-in");
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        logger.info(
                "Searching hotels for city: {} | Dates: {} to {} | Guests: {}",
                city, checkIn, checkOut, numAdults
        );

        List<HotelDto> hotels = HotelCatalog.hotelsFor(city);

        if (hotels.isEmpty()) {
            hotels = fallbackHotels(city);
        }

        double guestMultiplier = 1 + ((numAdults - 1) * 0.20);

        for (HotelDto hotel : hotels) {
            if (hotel.getPrice() != null) {
                double basePriceForNights =
                        hotel.getPrice() * nights;

                double finalPrice = Math.round(
                        (basePriceForNights * guestMultiplier) * 100.0
                ) / 100.0;

                hotel.setPrice(finalPrice);
            }
        }

        return hotels;
    }

    private List<HotelDto> fallbackHotels(String city) {
        GooglePlaces place =
                googlePlacesService.findPlaceByCity(city);

        boolean hasCityLocation = place != null
                && place.getPlaceId() != null
                && !place.getPlaceId().isBlank()
                && !place.getPlaceId().startsWith("unknown_")
                && validCoordinates(
                place.getLatitude(),
                place.getLongitude()
        );

        List<HotelDto> hotels = MockHotelUtils.generateMockHotels(
                city,
                hasCityLocation ? place.getLatitude() : 0.0,
                hasCityLocation ? place.getLongitude() : 0.0
        );

        for (HotelDto hotel : hotels) {
            if (!hasCityLocation
                    || !validCoordinates(
                    hotel.getLatitude(),
                    hotel.getLongitude()
            )) {

                hotel.setLatitude(null);
                hotel.setLongitude(null);
            }
        }

        return hotels;
    }

    private boolean validCoordinates(
            Double latitude,
            Double longitude
    ) {
        return latitude != null
                && Double.isFinite(latitude)
                && Math.abs(latitude) <= 90
                && longitude != null
                && Double.isFinite(longitude)
                && Math.abs(longitude) <= 180;
    }

    private LocalDate date(String value) {
        if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw invalid("Invalid hotel date");
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("Invalid hotel date");
        }
    }

    private ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, reason
        );
    }

    public Object searchHotelOffers(
            String hotelIds,
            String checkInDate,
            String checkOutDate,
            Integer adults
    ) {
        return List.of();
    }
}