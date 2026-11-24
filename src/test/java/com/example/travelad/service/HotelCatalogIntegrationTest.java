package com.example.travelad.service;

import com.example.travelad.dto.HotelDto;
import com.example.travelad.utils.HotelCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HotelCatalogIntegrationTest {

    private final GooglePlacesService places =
            mock(GooglePlacesService.class);

    private final HotelsService service =
            new HotelsService(places);

    @ParameterizedTest
    @ValueSource(strings = {
            "London, England, United Kingdom",
            "Paris, Île-de-France, France",
            "New York, New York, United States",
            "Cape Town, Western Cape, South Africa",
            "Reykjavik, Capital Region, Iceland",
            "Tokyo, Japan",
            "Roma, Lazio, Italy",
            "Barcelona, Catalonia, Spain",
            "Madrid, Spain",
            "Berlin, State of Berlin, Germany",
            "Amsterdam, North Holland, The Netherlands",
            "Vienna, Austria",
            "Prague, Czechia",
            "Budapest, Hungary",
            "Lisbon, Portugal",
            "Athens, Attica, Greece",
            "Istanbul, Turkey",
            "Dubai, United Arab Emirates",
            "Singapore, Singapore",
            "Hong Kong, Hong Kong",
            "Bangkok, Thailand",
            "Seoul, South Korea",
            "Sydney, New South Wales, Australia",
            "Melbourne, Victoria, Australia",
            "Los Angeles, California, United States",
            "San Francisco, California, United States",
            "Toronto, Ontario, Canada",
            "Ciudad de México, Mexico",
            "Rio de Janeiro, Brazil",
            "Buenos Aires, Buenos Aires F.D., Argentina"
    })
    void supportedCitiesReturnCatalogHotelsWithoutSupplierCalls(
            String city
    ) {
        List<HotelDto> hotels = service.searchHotelsByCityName(
                city, null, null, 1
        );

        assertTrue(hotels.size() >= 3);

        assertEquals(
                hotels.size(),
                hotels.stream()
                        .map(HotelDto::getHotelId)
                        .distinct()
                        .count()
        );

        for (HotelDto hotel : hotels) {
            assertTrue(hotel.getHotelId().startsWith("CATALOG_Q"));

            assertEquals(
                    "/hotel-images/"
                            + hotel.getHotelId().substring(8)
                            + ".webp",
                    hotel.getImageUrl()
            );

            assertNotNull(hotel.getLatitude());
            assertNotNull(hotel.getLongitude());
            assertNotNull(hotel.getPhotoAttribution());
            assertNull(hotel.getRating());
            assertTrue(hotel.getPrice() > 0);
        }

        verifyNoInteractions(places);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Paris, Texas, United States",
            "London, Ontario, Canada",
            "Melbourne, Florida, United States",
            "Paris",
            "Paris, Nevada, France"
    })
    void unrelatedOrUnqualifiedCitiesDoNotReceiveAnotherCityCatalog(
            String city
    ) {
        assertTrue(HotelCatalog.hotelsFor(city).isEmpty());

        List<HotelDto> hotels = service.searchHotelsByCityName(
                city, null, null, 1
        );

        assertEquals(25, hotels.size());

        assertTrue(hotels.stream().allMatch(hotel ->
                hotel.getHotelId().startsWith("MOCK_")
        ));
    }

    @Test
    void catalogRateIsStableAndDatePricingDoesNotMutateFutureResponses() {
        String city = "Paris, France";

        String start = LocalDate.now()
                .plusDays(5)
                .toString();

        String end = LocalDate.parse(start)
                .plusDays(3)
                .toString();

        HotelDto base = service.searchHotelsByCityName(
                city, null, null, 1
        ).getFirst();

        HotelDto threeNights = service.searchHotelsByCityName(
                city, start, end, 2
        ).getFirst();

        HotelDto repeated = service.searchHotelsByCityName(
                city, null, null, 1
        ).getFirst();

        assertEquals(
                base.getHotelId(),
                threeNights.getHotelId()
        );

        assertEquals(
                Math.round(base.getPrice() * 3 * 1.2 * 100) / 100.0,
                threeNights.getPrice(),
                0.001
        );

        assertEquals(base.getPrice(), repeated.getPrice());

        verifyNoInteractions(places);
    }
}