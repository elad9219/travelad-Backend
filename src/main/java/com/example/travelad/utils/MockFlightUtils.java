package com.example.travelad.utils;

import com.example.travelad.dto.FlightOfferDto;
import com.example.travelad.dto.FlightSegmentDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MockFlightUtils {

    private static final Random random = new Random();

    // Airline Logos Base URL
    private static final String LOGO_BASE_URL = "https://pics.avs.io/200/200/";

    // --- CARRIERS LISTS ---
    private static final List<String> ISRAEL_CARRIERS = Arrays.asList("LY", "IZ", "6H");
    private static final List<String> UK_CARRIERS = Arrays.asList("BA", "U2", "VS", "W6");
    private static final List<String> FRANCE_CARRIERS = Arrays.asList("AF", "TO", "U2");
    private static final List<String> GERMANY_CARRIERS = Arrays.asList("LH", "EW");
    private static final List<String> ITALY_CARRIERS = Arrays.asList("AZ", "FR", "W6");
    private static final List<String> GREECE_CYPRUS_CARRIERS = Arrays.asList("A3", "CY", "FR", "IZ", "6H");
    private static final List<String> LOW_COST_EUROPE = Arrays.asList("FR", "W6", "U2");
    private static final List<String> US_CARRIERS = Arrays.asList("DL", "UA", "AA");
    private static final List<String> ASIA_CARRIERS = Arrays.asList("EK", "TK", "ET", "AI", "CX");

    // --- HUBS FOR CONNECTIONS ---
    private static final List<String> EUROPE_HUBS = Arrays.asList("LHR", "CDG", "FRA", "MUC", "FCO", "ZRH", "IST");
    private static final List<String> ASIA_HUBS = Arrays.asList("DXB", "IST", "BKK");

    private enum Region {
        EUROPE, US_EAST, US_WEST, ASIA, NEAR_EAST, UNKNOWN
    }

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
        try { Thread.sleep(800 + random.nextInt(600)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        List<FlightOfferDto> offers = new ArrayList<>();
        int count = 10 + random.nextInt(5);
        Region region = getRegionByCode(destination);
        int passengers = Integer.parseInt(adults);

        for (int i = 0; i < count; i++) {
            FlightOfferDto offer = new FlightOfferDto();
            boolean isDirect = shouldBeDirect(region);

            List<FlightSegmentDto> outboundSegments = generateSmartSegments(origin, destination, departDate, isDirect, region);
            offer.setOutboundSegments(outboundSegments);
            offer.setOutboundDuration(calculateTotalDuration(outboundSegments));
            offer.setSegments(outboundSegments);

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
            case NEAR_EAST: return rand > 0.05;
            case EUROPE: return rand > 0.2;
            case US_EAST: return rand > 0.5;
            case US_WEST: return false;
            case ASIA: return rand > 0.7;
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
        if (isDirect) basePrice *= 1.15;
        return Math.floor(basePrice * passengers);
    }

    private static String pickRelevantCarrier(String destination, Region region) {
        List<String> options = new ArrayList<>();
        options.add("LY");

        String dest = destination.toUpperCase();

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
        } else {
            switch (region) {
                case EUROPE:
                    options.addAll(LOW_COST_EUROPE);
                    options.add("LH");
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
            String hub = pickHub(region);

            if (hub.equals("IST")) carrier = "TK";
            if (hub.equals("DXB")) carrier = "EK";

            int leg1Min = 240 + random.nextInt(60);
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

    private static String calculateTotalDuration(List<FlightSegmentDto> segments) {
        if (segments.isEmpty()) return "PT0H0M";
        LocalDateTime start = LocalDateTime.parse(segments.get(0).getDepartureDate());
        LocalDateTime end = LocalDateTime.parse(segments.get(segments.size() - 1).getArrivalDate());
        long diff = java.time.Duration.between(start, end).toMinutes();
        return String.format("PT%dH%dM", diff / 60, diff % 60);
    }
}