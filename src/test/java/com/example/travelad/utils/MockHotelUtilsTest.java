package com.example.travelad.utils;

import com.example.travelad.dto.HotelDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockHotelUtilsTest {

    private List<HotelDto> london() {
        return MockHotelUtils.generateMockHotels(
                "London, United Kingdom",
                51.5074,
                -0.1278
        );
    }

    @Test
    void generatesTwentyFiveHotels() {
        assertEquals(25, london().size());
    }

    @Test
    void hotelNamesAreUnique() {
        assertEquals(
                25L,
                london().stream()
                        .map(HotelDto::getName)
                        .distinct()
                        .count()
        );
    }

    @Test
    void hotelImagesAreUnique() {
        assertEquals(
                25L,
                london().stream()
                        .map(HotelDto::getImageUrl)
                        .distinct()
                        .count()
        );
    }

    @Test
    void hotelIdsAreUnique() {
        assertEquals(
                25L,
                london().stream()
                        .map(HotelDto::getHotelId)
                        .distinct()
                        .count()
        );
    }

    @Test
    void repeatedSearchesReturnConsistentHotels() {
        List<HotelDto> first = london();
        List<HotelDto> second = london();

        for (int i = 0; i < first.size(); i++) {
            HotelDto a = first.get(i);
            HotelDto b = second.get(i);

            assertNotSame(a, b);
            assertEquals(a.getHotelId(), b.getHotelId());
            assertEquals(a.getName(), b.getName());
            assertEquals(a.getPrice(), b.getPrice());
            assertEquals(a.getRating(), b.getRating());
            assertEquals(a.getImageUrl(), b.getImageUrl());
            assertEquals(a.getLatitude(), b.getLatitude());
            assertEquals(a.getLongitude(), b.getLongitude());
        }
    }

    @Test
    void cityCapitalizationDoesNotChangeHotelIdentity() {
        List<HotelDto> first = london();

        List<HotelDto> second =
                MockHotelUtils.generateMockHotels(
                        "london, united kingdom",
                        51.5074,
                        -0.1278
                );

        assertEquals(
                first.stream().map(HotelDto::getHotelId).toList(),
                second.stream().map(HotelDto::getHotelId).toList()
        );
    }

    @Test
    void differentCitiesHaveDifferentHotelIds() {
        List<String> londonIds = london().stream()
                .map(HotelDto::getHotelId)
                .toList();

        List<HotelDto> paris =
                MockHotelUtils.generateMockHotels(
                        "Paris, France",
                        48.8566,
                        2.3522
                );

        assertTrue(
                paris.stream().noneMatch(
                        hotel -> londonIds.contains(hotel.getHotelId())
                )
        );
    }

    @Test
    void pricesAndRatingsRemainWithinDemoRanges() {
        for (HotelDto hotel : london()) {
            assertTrue(hotel.getPrice() >= 80);
            assertTrue(hotel.getPrice() <= 429);
            assertTrue(hotel.getRating() >= 6.0);
            assertTrue(hotel.getRating() <= 9.5);
        }
    }

    @Test
    void coordinatesRemainValidNearGeographicBoundaries() {
        List<HotelDto> hotels =
                MockHotelUtils.generateMockHotels(
                        "Boundary City",
                        90.0,
                        180.0
                );

        for (HotelDto hotel : hotels) {
            assertTrue(Math.abs(hotel.getLatitude()) <= 90);
            assertTrue(Math.abs(hotel.getLongitude()) <= 180);
        }
    }
}