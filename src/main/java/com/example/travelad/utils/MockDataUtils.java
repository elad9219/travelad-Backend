package com.example.travelad.utils;

import com.example.travelad.dto.FlightOfferDto;
import com.example.travelad.dto.FlightSegmentDto;
import com.example.travelad.dto.HotelDto;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MockDataUtils {

    private static final Random random = new Random();

    // Airline Logos Base URL
    private static final String LOGO_BASE_URL = "https://pics.avs.io/200/200/";

    // --- CARRIERS ---
    private static final List<String> EUROPE_CARRIERS = Arrays.asList("LY", "BA", "AF", "LH", "LX", "AZ", "U2", "FR"); // El Al, BA, Air France, Lufthansa, Swiss, Alitalia, EasyJet, Ryanair
    private static final List<String> US_CARRIERS = Arrays.asList("LY", "DL", "UA", "AA"); // El Al, Delta, United, American
    private static final List<String> ASIA_CARRIERS = Arrays.asList("LY", "EK", "TK", "ET", "AI"); // El Al, Emirates, Turkish, Ethiopian, Air India

    // --- HUBS FOR CONNECTIONS ---
    private static final List<String> EUROPE_HUBS = Arrays.asList("LHR", "CDG", "FRA", "MUC", "FCO", "ZRH", "IST");
    private static final List<String> US_HUBS = Arrays.asList("JFK", "EWR");
    private static final List<String> ASIA_HUBS = Arrays.asList("DXB", "IST", "BKK");

    // --- REGION MAPPING (Simple Database) ---
    private enum Region {
        EUROPE, US_EAST, US_WEST, ASIA, NEAR_EAST, UNKNOWN
    }

    private static Region getRegionByCode(String code) {
        if (code == null) return Region.UNKNOWN;
        code = code.toUpperCase();

        // Europe Major Cities
        if (Arrays.asList("LON", "LHR", "LGW", "LTN", "PAR", "CDG", "ORY", "FRA", "MUC", "BER", "ROM", "FCO", "MIL", "MXP", "MAD", "BCN", "AMS", "ZRH", "VIE", "PRG", "BUD", "ATH").contains(code)) {
            return Region.EUROPE;
        }
        // US East Coast
        if (Arrays.asList("NYC", "JFK", "EWR", "BOS", "IAD", "MIA", "YYZ").contains(code)) {
            return Region.US_EAST;
        }
        // US West Coast & Others
        if (Arrays.asList("LAX", "SFO", "LAS", "SEA", "ORD", "DFW").contains(code)) {
            return Region.US_WEST;
        }
        // Asia / East
        if (Arrays.asList("BKK", "HKG", "TYO", "NRT", "HND", "DXB", "DEL", "BOM").contains(code)) {
            return Region.ASIA;
        }
        // Near East (Cyprus, Greece nearby, Eilat)
        if (Arrays.asList("LCA", "PFO", "ETM", "VDA").contains(code)) {
            return Region.NEAR_EAST;
        }

        return Region.UNKNOWN; // Default fallback
    }

    public static List<FlightOfferDto> generateMockFlights(String origin, String destination, String departDate, String returnDate, String adults) {
        // Simulate network latency
        try { Thread.sleep(800 + random.nextInt(600)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        List<FlightOfferDto> offers = new ArrayList<>();
        int count = 10 + random.nextInt(5); // 10 to 14 results
        Region region = getRegionByCode(destination);
        int passengers = Integer.parseInt(adults);

        for (int i = 0; i < count; i++) {
            FlightOfferDto offer = new FlightOfferDto();

            // Determine flight type (Direct/Connection) based on Region
            boolean isDirect = shouldBeDirect(region);

            // Generate Outbound
            List<FlightSegmentDto> outboundSegments = generateSmartSegments(origin, destination, departDate, isDirect, region);
            offer.setOutboundSegments(outboundSegments);
            offer.setOutboundDuration(calculateTotalDuration(outboundSegments));
            offer.setSegments(outboundSegments); // Backward compatibility

            // Generate Return (if needed)
            if (returnDate != null && !returnDate.isEmpty()) {
                List<FlightSegmentDto> returnSegments = generateSmartSegments(destination, origin, returnDate, isDirect, region);
                offer.setReturnSegments(returnSegments);
                offer.setReturnDuration(calculateTotalDuration(returnSegments));
            }

            // Calculate Realistic Price
            double price = calculatePrice(region, isDirect, passengers);
            offer.setPrice(price);

            offers.add(offer);
        }

        // Sort by price to look realistic (cheapest first)
        offers.sort(Comparator.comparingDouble(FlightOfferDto::getPrice));

        return offers;
    }

    private static boolean shouldBeDirect(Region region) {
        double rand = random.nextDouble();
        switch (region) {
            case NEAR_EAST: return rand > 0.1; // 90% Direct
            case EUROPE: return rand > 0.3; // 70% Direct
            case US_EAST: return rand > 0.6; // 40% Direct (EL AL, Delta etc)
            case US_WEST: return false; // almost always connection from TLV
            case ASIA: return rand > 0.7; // 30% Direct (El Al to BKK/Tokyo)
            default: return rand > 0.5;
        }
    }

    private static double calculatePrice(Region region, boolean isDirect, int passengers) {
        double basePrice;
        switch (region) {
            case NEAR_EAST: basePrice = 100 + random.nextInt(150); break;
            case EUROPE: basePrice = 300 + random.nextInt(300); break;
            case US_EAST: basePrice = 800 + random.nextInt(500); break;
            case US_WEST: basePrice = 1100 + random.nextInt(400); break;
            case ASIA: basePrice = 700 + random.nextInt(400); break;
            default: basePrice = 400 + random.nextInt(400);
        }

        // Direct flights are usually more expensive
        if (isDirect) basePrice *= 1.2;

        return Math.floor(basePrice * passengers);
    }

    private static List<FlightSegmentDto> generateSmartSegments(String from, String to, String dateStr, boolean isDirect, Region region) {
        List<FlightSegmentDto> segments = new ArrayList<>();

        // Pick a realistic carrier
        String carrier = pickCarrier(region);

        // Departure time (random hour between 05:00 and 23:00)
        int hour = 5 + random.nextInt(19);
        int minute = random.nextInt(12) * 5;
        LocalDateTime depTime = LocalDate.parse(dateStr).atTime(hour, minute);

        if (isDirect) {
            // Direct Flight Logic
            int flightMinutes = getBaseFlightMinutes(region);
            flightMinutes += (random.nextInt(60) - 30); // Add jitter +/- 30 mins

            LocalDateTime arrTime = depTime.plusMinutes(flightMinutes);
            segments.add(createSegment(from, to, depTime, arrTime, carrier));
        } else {
            // Connection Flight Logic
            String hub = pickHub(region);

            // Leg 1: Origin -> Hub
            int leg1Min = getDurationToHub(from, hub);
            LocalDateTime arr1 = depTime.plusMinutes(leg1Min);
            segments.add(createSegment(from, hub, depTime, arr1, carrier));

            // Layover (1.5 hours to 5 hours)
            int layoverMin = 90 + random.nextInt(210);
            LocalDateTime dep2 = arr1.plusMinutes(layoverMin);

            // Leg 2: Hub -> Dest
            int leg2Min = getBaseFlightMinutes(region) - leg1Min + 60; // Rough approximation
            if (leg2Min < 60) leg2Min = 60 + random.nextInt(60); // Minimum 1 hour flight

            // If US West, Leg 2 is long. If Asia, Leg 2 is long.
            if (region == Region.US_WEST) leg2Min = 300 + random.nextInt(120);

            LocalDateTime arr2 = dep2.plusMinutes(leg2Min);

            // Sometimes change carrier for second leg? No, keep simple for now.
            segments.add(createSegment(hub, to, dep2, arr2, carrier));
        }
        return segments;
    }

    private static String pickCarrier(Region region) {
        if (random.nextDouble() > 0.7) return "LY"; // 30% chance of El Al everywhere
        List<String> list;
        switch (region) {
            case EUROPE: list = EUROPE_CARRIERS; break;
            case US_EAST:
            case US_WEST: list = US_CARRIERS; break;
            case ASIA: list = ASIA_CARRIERS; break;
            default: list = EUROPE_CARRIERS;
        }
        return list.get(random.nextInt(list.size()));
    }

    private static String pickHub(Region region) {
        List<String> list;
        switch (region) {
            case US_EAST:
            case US_WEST:
            case EUROPE: list = EUROPE_HUBS; break; // Stop in Europe on way to US
            case ASIA: list = ASIA_HUBS; break;
            default: list = EUROPE_HUBS;
        }
        return list.get(random.nextInt(list.size()));
    }

    private static int getBaseFlightMinutes(Region region) {
        switch (region) {
            case NEAR_EAST: return 60 + random.nextInt(30); // ~1h (Cyprus)
            case EUROPE: return 240 + random.nextInt(60); // ~4-5h
            case US_EAST: return 660 + random.nextInt(60); // ~11-12h
            case US_WEST: return 900 + random.nextInt(60); // ~15h
            case ASIA: return 600 + random.nextInt(120); // ~10-12h
            default: return 180 + random.nextInt(120); // Generic 3-5h
        }
    }

    // Rough estimate from TLV to common hubs
    private static int getDurationToHub(String from, String hub) {
        if (ASIA_HUBS.contains(hub)) return 180 + random.nextInt(60); // To Dubai/Turkey
        return 240 + random.nextInt(60); // To Europe hubs
    }

    private static FlightSegmentDto createSegment(String from, String to, LocalDateTime dep, LocalDateTime arr, String carrier) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        long diff = java.time.Duration.between(dep, arr).toMinutes();
        String durationStr = String.format("PT%dH%dM", diff / 60, diff % 60);

        // Random flight number
        String flightNumber = String.valueOf(100 + random.nextInt(899));

        // Aircraft types
        String[] aircrafts = {"Boeing 737-800", "Airbus A320", "Boeing 787 Dreamliner", "Airbus A350", "Boeing 777"};
        String aircraft = aircrafts[random.nextInt(aircrafts.length)];
        String aircraftCode = aircraft.split(" ")[1].substring(0, 3); // simplistic code

        return new FlightSegmentDto(
                from,
                to,
                dep.format(formatter),
                arr.format(formatter),
                durationStr,
                carrier,
                carrier + flightNumber,
                aircraftCode,
                aircraft,
                "1", // Terminal
                "1", // Terminal
                LOGO_BASE_URL + carrier + ".png"
        );
    }

    private static String calculateTotalDuration(List<FlightSegmentDto> segments) {
        if (segments.isEmpty()) return "PT0H0M";
        LocalDateTime start = LocalDateTime.parse(segments.get(0).getDepartureDate());
        LocalDateTime end = LocalDateTime.parse(segments.get(segments.size() - 1).getArrivalDate());
        long diff = java.time.Duration.between(start, end).toMinutes();
        return String.format("PT%dH%dM", diff / 60, diff % 60);
    }

    // --- HOTELS MOCK (Kept as is from your previous code) ---
    public static List<HotelDto> generateMockHotels(String city, double lat, double lon) {
        List<HotelDto> hotels = new ArrayList<>();
        String decodedCity = URLDecoder.decode(city, StandardCharsets.UTF_8);
        String[] brands = {"Grand", "Royal", "Plaza", "Hilton", "Marriott", "Hyatt", "Sheraton"};

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
            hotel.setIataCode(decodedCity.substring(0, Math.min(decodedCity.length(), 3)).toUpperCase());
            hotels.add(hotel);
        }
        return hotels;
    }
}