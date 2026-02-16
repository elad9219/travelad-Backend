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

    // --- CARRIERS LISTS ---
    // Israel
    private static final List<String> ISRAEL_CARRIERS = Arrays.asList("LY", "IZ", "6H"); // El Al, Arkia, Israir

    // Europe Specific
    private static final List<String> UK_CARRIERS = Arrays.asList("BA", "U2", "VS", "W6"); // British, EasyJet, Virgin, Wizz
    private static final List<String> FRANCE_CARRIERS = Arrays.asList("AF", "TO", "U2"); // Air France, Transavia, EasyJet
    private static final List<String> GERMANY_CARRIERS = Arrays.asList("LH", "EW"); // Lufthansa, Eurowings
    private static final List<String> ITALY_CARRIERS = Arrays.asList("AZ", "FR", "W6"); // ITA, Ryanair, Wizz
    private static final List<String> GREECE_CYPRUS_CARRIERS = Arrays.asList("A3", "CY", "FR", "IZ", "6H"); // Aegean, Cyprus Airways, Ryanair, Arkia, Israir

    // Low Cost General (Fallback for Europe)
    private static final List<String> LOW_COST_EUROPE = Arrays.asList("FR", "W6", "U2"); // Ryanair, Wizz, EasyJet

    // US
    private static final List<String> US_CARRIERS = Arrays.asList("DL", "UA", "AA"); // Delta, United, American

    // Asia
    private static final List<String> ASIA_CARRIERS = Arrays.asList("EK", "TK", "ET", "AI", "CX"); // Emirates, Turkish, Ethiopian, Air India, Cathay

    // --- HUBS FOR CONNECTIONS ---
    private static final List<String> EUROPE_HUBS = Arrays.asList("LHR", "CDG", "FRA", "MUC", "FCO", "ZRH", "IST");
    private static final List<String> ASIA_HUBS = Arrays.asList("DXB", "IST", "BKK");

    private enum Region {
        EUROPE, US_EAST, US_WEST, ASIA, NEAR_EAST, UNKNOWN
    }

    // Identify region based on city code
    private static Region getRegionByCode(String code) {
        if (code == null) return Region.UNKNOWN;
        code = code.toUpperCase();

        if (Arrays.asList("LCA", "PFO", "ATH", "HER", "RHO", "ETM", "VDA").contains(code)) return Region.NEAR_EAST;
        if (Arrays.asList("LON", "LHR", "LGW", "LTN", "PAR", "CDG", "ORY", "FRA", "MUC", "BER", "ROM", "FCO", "MIL", "MXP", "MAD", "BCN", "AMS", "ZRH", "VIE", "PRG", "BUD").contains(code)) return Region.EUROPE;
        if (Arrays.asList("NYC", "JFK", "EWR", "BOS", "IAD", "MIA", "YYZ").contains(code)) return Region.US_EAST;
        if (Arrays.asList("LAX", "SFO", "LAS", "SEA", "ORD", "DFW").contains(code)) return Region.US_WEST;
        if (Arrays.asList("BKK", "HKG", "TYO", "NRT", "HND", "DXB", "DEL", "BOM").contains(code)) return Region.ASIA;

        return Region.UNKNOWN;
    }

    public static List<FlightOfferDto> generateMockFlights(String origin, String destination, String departDate, String returnDate, String adults) {
        // Simulate network latency
        try { Thread.sleep(800 + random.nextInt(600)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        List<FlightOfferDto> offers = new ArrayList<>();
        int count = 10 + random.nextInt(5);
        Region region = getRegionByCode(destination);
        int passengers = Integer.parseInt(adults);

        for (int i = 0; i < count; i++) {
            FlightOfferDto offer = new FlightOfferDto();

            boolean isDirect = shouldBeDirect(region);

            // Generate Outbound
            List<FlightSegmentDto> outboundSegments = generateSmartSegments(origin, destination, departDate, isDirect, region);
            offer.setOutboundSegments(outboundSegments);
            offer.setOutboundDuration(calculateTotalDuration(outboundSegments));
            offer.setSegments(outboundSegments);

            // Generate Return (if needed)
            if (returnDate != null && !returnDate.isEmpty()) {
                List<FlightSegmentDto> returnSegments = generateSmartSegments(destination, origin, returnDate, isDirect, region);
                offer.setReturnSegments(returnSegments);
                offer.setReturnDuration(calculateTotalDuration(returnSegments));
            }

            double price = calculatePrice(region, isDirect, passengers);
            offer.setPrice(price);

            offers.add(offer);
        }

        offers.sort(Comparator.comparingDouble(FlightOfferDto::getPrice));
        return offers;
    }

    private static boolean shouldBeDirect(Region region) {
        double rand = random.nextDouble();
        switch (region) {
            case NEAR_EAST: return rand > 0.05; // 95% Direct (Cyprus/Greece)
            case EUROPE: return rand > 0.2; // 80% Direct
            case US_EAST: return rand > 0.5; // 50% Direct
            case US_WEST: return false; // Always connection
            case ASIA: return rand > 0.7; // 30% Direct
            default: return rand > 0.5;
        }
    }

    private static double calculatePrice(Region region, boolean isDirect, int passengers) {
        double basePrice;
        switch (region) {
            case NEAR_EAST: basePrice = 100 + random.nextInt(150); break;
            case EUROPE: basePrice = 250 + random.nextInt(350); break;
            case US_EAST: basePrice = 800 + random.nextInt(500); break;
            case US_WEST: basePrice = 1100 + random.nextInt(400); break;
            case ASIA: basePrice = 700 + random.nextInt(400); break;
            default: basePrice = 400 + random.nextInt(400);
        }
        if (isDirect) basePrice *= 1.15; // Direct is slightly more expensive
        return Math.floor(basePrice * passengers);
    }

    // --- CORE LOGIC: Pick relevant airline based on destination ---
    private static String pickRelevantCarrier(String destination, Region region) {
        List<String> options = new ArrayList<>();

        // Always include El Al as an option for TLV flights
        options.add("LY");

        String dest = destination.toUpperCase();

        // 1. Specific City Logic
        if (dest.contains("LON") || dest.contains("LHR") || dest.contains("LTN") || dest.contains("LGW")) {
            options.addAll(UK_CARRIERS);
        } else if (dest.contains("PAR") || dest.contains("CDG") || dest.contains("ORY")) {
            options.addAll(FRANCE_CARRIERS);
        } else if (dest.contains("FRA") || dest.contains("MUC") || dest.contains("BER")) {
            options.addAll(GERMANY_CARRIERS);
        } else if (dest.contains("ROM") || dest.contains("FCO") || dest.contains("MIL")) {
            options.addAll(ITALY_CARRIERS);
        } else if (dest.contains("LCA") || dest.contains("PFO") || dest.contains("ATH")) {
            options.addAll(GREECE_CYPRUS_CARRIERS);
        }
        // 2. Regional Logic (if no specific city matched)
        else {
            switch (region) {
                case EUROPE:
                    options.addAll(LOW_COST_EUROPE); // EasyJet, Wizz, etc.
                    options.add("LH"); // Lufthansa is common
                    break;
                case US_EAST:
                case US_WEST:
                    options.addAll(US_CARRIERS);
                    break;
                case ASIA:
                    options.addAll(ASIA_CARRIERS);
                    break;
                default:
                    options.addAll(LOW_COST_EUROPE);
            }
        }

        return options.get(random.nextInt(options.size()));
    }

    private static List<FlightSegmentDto> generateSmartSegments(String from, String to, String dateStr, boolean isDirect, Region region) {
        List<FlightSegmentDto> segments = new ArrayList<>();

        // Determine main carrier based on destination (or origin if returning)
        String targetForCarrier = from.equals("TLV") ? to : from;
        String carrier = pickRelevantCarrier(targetForCarrier, region);

        int hour = 5 + random.nextInt(19);
        int minute = random.nextInt(12) * 5;
        LocalDateTime depTime = LocalDate.parse(dateStr).atTime(hour, minute);

        if (isDirect) {
            int flightMinutes = getBaseFlightMinutes(region) + (random.nextInt(60) - 30);
            LocalDateTime arrTime = depTime.plusMinutes(flightMinutes);
            segments.add(createSegment(from, to, depTime, arrTime, carrier));
        } else {
            // Connection Logic
            String hub = pickHub(region);

            // Special case: If connecting via Istanbul, maybe switch carrier to Turkish?
            if (hub.equals("IST")) carrier = "TK";
            if (hub.equals("DXB")) carrier = "EK";

            int leg1Min = 240 + random.nextInt(60); // To Hub
            LocalDateTime arr1 = depTime.plusMinutes(leg1Min);
            segments.add(createSegment(from, hub, depTime, arr1, carrier));

            int layoverMin = 90 + random.nextInt(210);
            LocalDateTime dep2 = arr1.plusMinutes(layoverMin);

            int leg2Min = getBaseFlightMinutes(region) - leg1Min + 60;
            if (leg2Min < 60) leg2Min = 60 + random.nextInt(60);

            LocalDateTime arr2 = dep2.plusMinutes(leg2Min);
            segments.add(createSegment(hub, to, dep2, arr2, carrier));
        }
        return segments;
    }

    private static String pickHub(Region region) {
        List<String> list;
        switch (region) {
            case US_EAST:
            case US_WEST:
            case EUROPE: list = EUROPE_HUBS; break;
            case ASIA: list = ASIA_HUBS; break;
            default: list = EUROPE_HUBS;
        }
        return list.get(random.nextInt(list.size()));
    }

    private static int getBaseFlightMinutes(Region region) {
        switch (region) {
            case NEAR_EAST: return 60 + random.nextInt(30);
            case EUROPE: return 240 + random.nextInt(60);
            case US_EAST: return 660 + random.nextInt(60);
            case US_WEST: return 900 + random.nextInt(60);
            case ASIA: return 600 + random.nextInt(120);
            default: return 180 + random.nextInt(120);
        }
    }

    private static FlightSegmentDto createSegment(String from, String to, LocalDateTime dep, LocalDateTime arr, String carrier) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        long diff = java.time.Duration.between(dep, arr).toMinutes();
        String durationStr = String.format("PT%dH%dM", diff / 60, diff % 60);
        String flightNumber = String.valueOf(100 + random.nextInt(899));

        String[] aircrafts = {"Boeing 737-800", "Airbus A320", "Boeing 787 Dreamliner", "Airbus A321neo", "Boeing 777"};
        String aircraft = aircrafts[random.nextInt(aircrafts.length)];
        // Small override for Israir/Arkia to use smaller planes usually
        if (carrier.equals("IZ") || carrier.equals("6H")) {
            aircraft = "Airbus A320";
        }

        String aircraftCode = aircraft.split(" ")[1].substring(0, 3);

        return new FlightSegmentDto(
                from, to, dep.format(formatter), arr.format(formatter), durationStr,
                carrier, carrier + flightNumber, aircraftCode, aircraft,
                "1", "1", LOGO_BASE_URL + carrier + ".png"
        );
    }

    // Calculate total duration helper
    private static String calculateTotalDuration(List<FlightSegmentDto> segments) {
        if (segments.isEmpty()) return "PT0H0M";
        LocalDateTime start = LocalDateTime.parse(segments.get(0).getDepartureDate());
        LocalDateTime end = LocalDateTime.parse(segments.get(segments.size() - 1).getArrivalDate());
        long diff = java.time.Duration.between(start, end).toMinutes();
        return String.format("PT%dH%dM", diff / 60, diff % 60);
    }

    // --- HOTELS MOCK (Unchanged) ---
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
                    "MOCK_" + i, "XX", "XX", hLat, hLon, price
            );
            hotel.setIataCode(decodedCity.substring(0, Math.min(decodedCity.length(), 3)).toUpperCase());
            hotels.add(hotel);
        }
        return hotels;
    }
}