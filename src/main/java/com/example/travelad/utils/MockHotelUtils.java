package com.example.travelad.utils;

import com.example.travelad.dto.HotelDto;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MockHotelUtils {

    private static final Random random = new Random();

    // 25 תמונות איכותיות של מלונות
    private static final List<String> HOTEL_IMAGES = Arrays.asList(
            "https://images.pexels.com/photos/164595/pexels-photo-164595.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/258154/pexels-photo-258154.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/271624/pexels-photo-271624.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/1134176/pexels-photo-1134176.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/262048/pexels-photo-262048.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/2034335/pexels-photo-2034335.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/271618/pexels-photo-271618.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/189296/pexels-photo-189296.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/594077/pexels-photo-594077.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/271619/pexels-photo-271619.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/1743229/pexels-photo-1743229.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/2869215/pexels-photo-2869215.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/261388/pexels-photo-261388.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/2736388/pexels-photo-2736388.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/1001965/pexels-photo-1001965.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/338504/pexels-photo-338504.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/271643/pexels-photo-271643.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/2506988/pexels-photo-2506988.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3201761/pexels-photo-3201761.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80",
            "https://images.pexels.com/photos/1579253/pexels-photo-1579253.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.unsplash.com/photo-1535827841776-24afc1e255ac?auto=format&fit=crop&w=800&q=80",
            "https://images.pexels.com/photos/2507010/pexels-photo-2507010.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=800&q=80"
    );

    private static final String[] HOTEL_PREFIXES = {"Grand", "Royal", "The", "Central", "Luxury", "Boutique", "Downtown", "Premium", "Oasis", "Urban"};
    private static final String[] HOTEL_SUFFIXES = {"Resort", "Plaza", "Hotel", "Suites", "Lodge", "Inn", "Spa & Resort", "Palace", "Gardens"};

    public static List<HotelDto> generateMockHotels(String city, double lat, double lon) {
        List<HotelDto> hotels = new ArrayList<>();
        String decodedCity = URLDecoder.decode(city, StandardCharsets.UTF_8);

        for (int i = 0; i < 25; i++) {
            double hLat = lat + (random.nextDouble() - 0.5) * 0.04;
            double hLon = lon + (random.nextDouble() - 0.5) * 0.04;
            double price = 80 + random.nextInt(350);

            String prefix = HOTEL_PREFIXES[random.nextInt(HOTEL_PREFIXES.length)];
            String suffix = HOTEL_SUFFIXES[random.nextInt(HOTEL_SUFFIXES.length)];
            String dynamicName = prefix + " " + decodedCity + " " + suffix;

            String imageUrl = HOTEL_IMAGES.get(random.nextInt(HOTEL_IMAGES.size()));

            // ציונים ריאליים יותר: בין 6.0 ל-9.5
            double rawRating = 6.0 + (random.nextDouble() * 3.5);
            double roundedRating = Math.round(rawRating * 10.0) / 10.0;

            HotelDto hotel = new HotelDto(
                    dynamicName,
                    "MOCK_" + i,
                    "XX",
                    "XX",
                    hLat,
                    hLon,
                    price,
                    imageUrl,
                    roundedRating
            );

            hotel.setIataCode(decodedCity.length() >= 3 ? decodedCity.substring(0, 3).toUpperCase() : "XXX");
            hotels.add(hotel);
        }
        return hotels;
    }
}