package com.example.travelad.utils;

import com.example.travelad.dto.HotelDto;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class MockHotelUtils {

    private static final int HOTEL_COUNT = 25;

    private static final List<String> HOTEL_IMAGES = createImages();

    private static final List<String> PREFIXES = List.of(
            "Grand", "Royal", "The", "Central", "Luxury",
            "Boutique", "Downtown", "Premium", "Oasis", "Urban"
    );

    private static final List<String> SUFFIXES = List.of(
            "Resort", "Plaza", "Hotel", "Suites", "Lodge",
            "Inn", "Spa & Resort", "Palace", "Gardens"
    );

    private static List<String> createImages() {
        List<String> images = new ArrayList<>();

        for (int id : new int[]{
                164595, 258154, 271624, 1134176, 262048,
                2034335, 271618, 189296, 594077, 271619,
                1743229, 2869215, 261388, 2736388, 1001965,
                338504, 271643, 2506988, 3201761, 1579253,
                2507010
        }) {
            images.add(
                    "https://images.pexels.com/photos/" + id
                            + "/pexels-photo-" + id
                            + ".jpeg?auto=compress&cs=tinysrgb&w=800"
            );
        }

        for (String id : List.of(
                "1590490360182-c33d57733427",
                "1535827841776-24afc1e255ac",
                "1584132967334-10e028bd69f7",
                "1520250497591-112f2f40a3f4"
        )) {
            images.add(
                    "https://images.unsplash.com/photo-" + id
                            + "?auto=format&fit=crop&w=800&q=80"
            );
        }

        return List.copyOf(images);
    }

    public static List<HotelDto> generateMockHotels(
            String city,
            double lat,
            double lon) {

        String cityKey = city.strip().toLowerCase(Locale.ROOT);

        UUID cityId = UUID.nameUUIDFromBytes(
                cityKey.getBytes(StandardCharsets.UTF_8)
        );

        Random random = new Random(
                cityId.getMostSignificantBits()
                        ^ cityId.getLeastSignificantBits()
        );

        List<String> names = new ArrayList<>();

        for (String prefix : PREFIXES) {
            for (String suffix : SUFFIXES) {
                names.add(
                        prefix + " " + city.strip() + " " + suffix
                );
            }
        }

        List<String> images = new ArrayList<>(HOTEL_IMAGES);

        Collections.shuffle(names, random);
        Collections.shuffle(images, random);

        List<HotelDto> hotels = new ArrayList<>(HOTEL_COUNT);

        for (int i = 0; i < HOTEL_COUNT; i++) {
            double hotelLat = Math.max(
                    -90,
                    Math.min(
                            90,
                            lat + (random.nextDouble() - 0.5) * 0.04
                    )
            );

            double hotelLon =
                    lon + (random.nextDouble() - 0.5) * 0.04;

            if (hotelLon > 180) {
                hotelLon -= 360;
            }

            if (hotelLon < -180) {
                hotelLon += 360;
            }

            double price = 80 + random.nextInt(350);

            double rating = Math.round(
                    (6.0 + random.nextDouble() * 3.5) * 10.0
            ) / 10.0;

            hotels.add(new HotelDto(
                    names.get(i),
                    "MOCK_" + cityId + "_" + i,
                    "XX",
                    "XX",
                    hotelLat,
                    hotelLon,
                    price,
                    images.get(i),
                    rating
            ));
        }

        return hotels;
    }
}