package com.example.travelad.utils;

import com.example.travelad.dto.FlightOfferDto;
import com.example.travelad.dto.FlightSegmentDto;
import com.example.travelad.dto.HotelDto;
import com.example.travelad.dto.RoomDto; // Ensure this is imported

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MockDataUtils {

    private static final Random random = new Random();

    // Common carriers in Ben Gurion Airport
    private static final List<String> CARRIERS = Arrays.asList("LY", "BA", "AF", "DL", "LH", "UA", "AA", "EK", "TK", "U2");

    // Logical connection hubs based on region
    private static final Map<String, List<String>> REGIONAL_HUBS = new HashMap<>();

    static {
        REGIONAL_HUBS.put("EUROPE", Arrays.asList("LHR", "CDG", "FRA", "MUC", "FCO", "ZRH", "IST"));
        REGIONAL_HUBS.put("AMERICA", Arrays.asList("LHR", "CDG", "FRA", "MAD", "JFK", "EWR"));
        REGIONAL_HUBS.put("ASIA", Arrays.asList("DXB", "BKK", "HKG", "SIN", "IST"));
    }

    public static List<FlightOfferDto> generateMockFlights(String origin, String destination, String departDate, String returnDate, String adults) {
        try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        List<FlightOfferDto> offers = new ArrayList<>();
        int count = 12;

        for (int i = 0; i < count; i++) {
            FlightOfferDto offer = new FlightOfferDto();
            double basePrice = 250 + random.nextInt(500);
            if (destination.length() > 2 && (destination.contains("JFK") || destination.contains("LAX"))) basePrice += 500;
            int passengers = Integer.parseInt(adults);
            offer.setPrice(basePrice * passengers);
            boolean isDirect = random.nextDouble() > 0.4;

            List<FlightSegmentDto> outboundSegments = generateSmartSegments(origin, destination, departDate, isDirect);
            offer.setOutboundSegments(outboundSegments);
            offer.setOutboundDuration(calculateTotalDuration(outboundSegments));
            offer.setSegments(outboundSegments);

            if (returnDate != null && !returnDate.isEmpty()) {
                List<FlightSegmentDto> returnSegments = generateSmartSegments(destination, origin, returnDate, isDirect);
                offer.setReturnSegments(returnSegments);
                offer.setReturnDuration(calculateTotalDuration(returnSegments));
            }
            offers.add(offer);
        }
        return offers;
    }

    private static List<FlightSegmentDto> generateSmartSegments(String from, String to, String dateStr, boolean isDirect) {
        List<FlightSegmentDto> segments = new ArrayList<>();
        String carrier = CARRIERS.get(random.nextInt(CARRIERS.size()));
        int hour = 6 + random.nextInt(16);
        int minute = random.nextInt(4) * 15;
        LocalDateTime depTime = LocalDate.parse(dateStr).atTime(hour, minute);

        if (isDirect) {
            int flightMinutes = 200 + random.nextInt(200);
            LocalDateTime arrTime = depTime.plusMinutes(flightMinutes);
            segments.add(createSegment(from, to, depTime, arrTime, carrier));
        } else {
            String hub = REGIONAL_HUBS.get("EUROPE").get(random.nextInt(REGIONAL_HUBS.get("EUROPE").size()));
            int f1 = 180 + random.nextInt(60);
            LocalDateTime arr1 = depTime.plusMinutes(f1);
            segments.add(createSegment(from, hub, depTime, arr1, carrier));
            int layover = 90 + random.nextInt(120);
            LocalDateTime dep2 = arr1.plusMinutes(layover);
            int f2 = 180 + random.nextInt(300);
            LocalDateTime arr2 = dep2.plusMinutes(f2);
            segments.add(createSegment(hub, to, dep2, arr2, carrier));
        }
        return segments;
    }

    private static FlightSegmentDto createSegment(String from, String to, LocalDateTime dep, LocalDateTime arr, String carrier) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        long diff = java.time.Duration.between(dep, arr).toMinutes();
        String durationStr = String.format("PT%dH%dM", diff / 60, diff % 60);
        return new FlightSegmentDto(from, to, dep.format(formatter), arr.format(formatter), durationStr, carrier, carrier + (100 + random.nextInt(900)), "738", "Boeing 737-800", "1", "1", "https://pics.avs.io/200/200/" + carrier + ".png");
    }

    private static String calculateTotalDuration(List<FlightSegmentDto> segments) {
        if (segments.isEmpty()) return "PT0H0M";
        LocalDateTime start = LocalDateTime.parse(segments.get(0).getDepartureDate());
        LocalDateTime end = LocalDateTime.parse(segments.get(segments.size() - 1).getArrivalDate());
        long diff = java.time.Duration.between(start, end).toMinutes();
        return String.format("PT%dH%dM", diff / 60, diff % 60);
    }

    public static List<HotelDto> generateMockHotels(String city, double lat, double lon) {
        List<HotelDto> hotels = new ArrayList<>();
        String decodedCity = URLDecoder.decode(city, StandardCharsets.UTF_8);
        String[] brands = {"Grand", "Royal", "Plaza", "Hilton", "Marriott", "Hyatt", "Sheraton"};
        String[] roomDescs = {"Deluxe Room, City View", "Superior Suite, 1 King Bed", "Standard Double Room", "Executive Lounge Access, Guest room", "Family Suite, 2 Queen Beds"};
        String[] bedTypes = {"KING", "QUEEN", "DOUBLE", "TWIN"};

        for (int i = 0; i < 15; i++) {
            double hLat = lat + (random.nextDouble() - 0.5) * 0.04;
            double hLon = lon + (random.nextDouble() - 0.5) * 0.04;
            double price = 100 + random.nextInt(400);

            HotelDto hotel = new HotelDto(
                    brands[i % brands.length] + " " + decodedCity,
                    "MOCK_" + i,
                    "XX",
                    "XX",
                    hLat,
                    hLon,
                    price
            );

            // Setting additional info for detailed view
            hotel.setIataCode(decodedCity.substring(0, Math.min(decodedCity.length(), 3)).toUpperCase());

            // To mimic the original detailed data, we could add a method or extend HotelDto
            // For now, let's assume we use the Price to show "Base Price" and "Total Price" (Price * 1.2 tax)
            hotels.add(hotel);
        }
        return hotels;
    }
}