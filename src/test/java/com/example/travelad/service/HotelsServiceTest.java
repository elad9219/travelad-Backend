package com.example.travelad.service;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.dto.HotelDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HotelsServiceTest {

    private final GooglePlacesService places =
            mock(GooglePlacesService.class);

    private final HotelsService service = new HotelsService(places);

    private GooglePlaces place(
            String id,
            Double latitude,
            Double longitude) {

        GooglePlaces place = new GooglePlaces();
        place.setPlaceId(id);
        place.setLatitude(latitude);
        place.setLongitude(longitude);
        return place;
    }

    private List<HotelDto> search() {
        return service.searchHotelsByCityName(
                "London", null, null, 1);
    }

    @Test
    void validCityLocationsKeepTheExistingNearbyDemoCoordinates() {
        when(places.findPlaceByCity("London"))
                .thenReturn(place("known-city", 51.5, -0.12));

        List<HotelDto> hotels = search();

        assertEquals(25, hotels.size());
        assertTrue(hotels.stream().allMatch(hotel ->
                hotel.getLatitude() != null
                        && hotel.getLongitude() != null
                        && Math.abs(hotel.getLatitude() - 51.5) <= 0.02
                        && Math.abs(hotel.getLongitude() + 0.12) <= 0.02));

        verify(places).findPlaceByCity("London");
    }

    @Test
    void aZeroCoordinateAxisIsNotMistakenForAnUnavailableLocation() {
        when(places.findPlaceByCity("London"))
                .thenReturn(place("known-city", 0.0, 30.0));

        assertTrue(search().stream().allMatch(hotel ->
                hotel.getLatitude() != null
                        && hotel.getLongitude() != null
                        && Math.abs(hotel.getLatitude()) <= 0.02
                        && Math.abs(hotel.getLongitude() - 30.0) <= 0.02));
    }

    @Test
    void unavailableCityMetadataKeepsHotelsWithoutInventingMapLocations() {
        List<HotelDto> hotels = search();

        assertEquals(25, hotels.size());
        assertTrue(hotels.stream().allMatch(hotel ->
                hotel.getLatitude() == null
                        && hotel.getLongitude() == null
                        && hotel.getName().contains("London")
                        && hotel.getPrice() > 0
                        && hotel.getImageUrl() != null));

        JsonNode serialized =
                new ObjectMapper().valueToTree(hotels.get(0));

        assertFalse(serialized.has("latitude"));
        assertFalse(serialized.has("longitude"));
    }

    static Stream<Arguments> unavailableLocations() {
        return Stream.of(
                Arguments.of("unknown_London", 0.0, 0.0),
                Arguments.of("unknown_London", 51.5, -0.12),
                Arguments.of(null, 51.5, -0.12),
                Arguments.of("", 51.5, -0.12),
                Arguments.of("known-city", null, -0.12),
                Arguments.of("known-city", Double.NaN, -0.12),
                Arguments.of(
                        "known-city",
                        51.5,
                        Double.POSITIVE_INFINITY),
                Arguments.of("known-city", 91.0, -0.12),
                Arguments.of("known-city", 51.5, 181.0)
        );
    }

    @ParameterizedTest
    @MethodSource("unavailableLocations")
    void invalidOrFallbackMetadataCannotBecomeHotelMapMarkers(
            String id,
            Double latitude,
            Double longitude) {

        when(places.findPlaceByCity("London"))
                .thenReturn(place(id, latitude, longitude));

        List<HotelDto> hotels = search();

        assertEquals(25, hotels.size());
        assertTrue(hotels.stream().allMatch(hotel ->
                hotel.getLatitude() == null
                        && hotel.getLongitude() == null));
    }
}