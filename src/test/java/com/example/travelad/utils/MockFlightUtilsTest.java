package com.example.travelad.utils;

import com.example.travelad.dto.FlightOfferDto;
import com.example.travelad.dto.FlightSegmentDto;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MockFlightUtilsTest {
    private static final String DEPARTURE = "2027-02-05";
    private static final String RETURN = "2027-02-10";
    private static final Set<String> LONDON = Set.of("LHR", "LGW", "LTN", "STN", "LCY");
    private static final Set<String> TOKYO = Set.of("HND", "NRT");

    @Test
    void metroSearchIncludesDirectAndConnectingOffersWithoutFakeAirportCodes() {
        List<FlightOfferDto> offers = search("TLV", "LON", RETURN, "1");
        assertEquals(12, offers.size());
        assertTrue(offers.stream().anyMatch(f -> f.getSegments().size() == 1));
        assertTrue(offers.stream().anyMatch(f -> f.getSegments().size() == 2));
        Set<String> ids = new HashSet<>();

        for (FlightOfferDto offer : offers) {
            assertTrue(ids.add(offer.getId()));
            assertEquals("MOCK", offer.getSource());
            List<FlightSegmentDto> segments = offer.getOutboundSegments();

            assertEquals("TLV", segments.getFirst().getOrigin());
            assertTrue(LONDON.contains(segments.getLast().getDestination()));

            if (segments.size() == 2) {
                assertFalse(LONDON.contains(segments.getFirst().getDestination()));
                assertNotEquals("TLV", segments.getFirst().getDestination());
            }

            assertEquals(segments, offer.getSegments());
            assertEquals(segments.getLast().getDestination(), offer.getReturnSegments().getFirst().getOrigin());
            assertEquals("TLV", offer.getReturnSegments().getLast().getDestination());
        }
    }

    @Test
    void resultsAreStableAndPricesScaleExactlyWithPassengerCount() {
        List<FlightOfferDto> first = search("TLV", "LON", RETURN, "1");
        List<FlightOfferDto> again = search("TLV", "LON", RETURN, "1");
        List<FlightOfferDto> three = search("TLV", "LON", RETURN, "3");

        assertFalse(first.isEmpty());
        assertEquals(first.size(), three.size());

        for (int i = 0; i < first.size(); i++) {
            FlightOfferDto a = first.get(i), b = again.get(i), c = three.get(i);

            assertEquals(a.getId(), b.getId());
            assertEquals(a.getId(), c.getId());
            assertEquals(a.getPrice(), b.getPrice());
            assertEquals(a.getPrice() * 3, c.getPrice());
            assertEquals(signature(a), signature(b));
            assertEquals(signature(a), signature(c));

            if (i > 0) assertTrue(first.get(i - 1).getPrice() <= a.getPrice());
        }
    }

    @Test
    void datesDurationsAndConnectionsAgreeAcrossTimeZones() throws Exception {
        Map<String, ZoneId> zones = zones();

        for (String destination : List.of("LON", "NYC", "TYO", "SYD", "AKL", "LCA")) {
            List<FlightOfferDto> offers = search("TLV", destination, RETURN, "1");
            assertFalse(offers.isEmpty(), destination);

            for (FlightOfferDto offer : offers) {
                List<FlightSegmentDto> out = offer.getOutboundSegments();
                List<FlightSegmentDto> back = offer.getReturnSegments();

                assertTrue(out.getFirst().getDepartureDate().startsWith(DEPARTURE));
                assertTrue(back.getFirst().getDepartureDate().startsWith(RETURN));

                checkLeg(out, offer.getOutboundDuration(), zones);
                checkLeg(back, offer.getReturnDuration(), zones);

                assertFalse(instant(back.getFirst().getDepartureDate(), back.getFirst().getOrigin(), zones)
                        .isBefore(instant(out.getLast().getArrivalDate(), out.getLast().getDestination(), zones)
                                .plus(Duration.ofHours(3))));
            }
        }
    }

    @Test
    void oneWayAndExplicitAirportSearchPreserveTheExistingContract() {
        List<FlightOfferDto> offers = search("TLV", "LHR", "", "2");
        assertFalse(offers.isEmpty());

        for (FlightOfferDto offer : offers) {
            assertEquals("LHR", offer.getSegments().getLast().getDestination());
            assertNull(offer.getReturnSegments());
            assertNull(offer.getReturnDuration());
            assertTrue(offer.getPrice() > 0);
        }
    }

    @Test
    void impossibleAndUnknownRoutesReturnEmptyInsteadOfInventedAirports() {
        assertTrue(search("TLV", "ZZZ", "", "1").isEmpty());
        assertTrue(search("TLV", "HFA", "", "1").isEmpty());
        assertTrue(search("LON", "LHR", "", "1").isEmpty());
        assertTrue(search("LHR", "LGW", "", "1").isEmpty());
    }

    @Test
    void longHaulOffersAreLongerAndMoreExpensiveAndUseSuitableAircraft() {
        List<FlightOfferDto> nearby = search("TLV", "LCA", "", "1");
        List<FlightOfferDto> far = search("TLV", "TYO", "", "1");

        assertFalse(nearby.isEmpty());
        assertFalse(far.isEmpty());
        assertTrue(far.getFirst().getPrice() > nearby.getFirst().getPrice());

        for (FlightOfferDto offer : far) {
            assertTrue(TOKYO.contains(offer.getSegments().getLast().getDestination()));
            assertTrue(Duration.parse(offer.getOutboundDuration()).toHours() >= 8);
        }

        for (FlightOfferDto offer : search("TLV", "LON", RETURN, "1")) {
            for (FlightSegmentDto segment : offer.getSegments()) {
                if (Set.of("U2", "W6").contains(segment.getCarrierCode())) {
                    assertTrue(segment.getAircraftFullName().startsWith("Airbus A32"));
                }

                assertTrue(segment.getFlightNumber().matches("[0-9]+"));
                assertNull(segment.getDepartureTerminal());
                assertNull(segment.getArrivalTerminal());
            }
        }
    }

    @Test
    void sameDayReturnNeverDepartsBeforeOutboundArrives() throws Exception {
        Map<String, ZoneId> zones = zones();
        List<FlightOfferDto> offers = search("TLV", "LCA", DEPARTURE, "1");

        assertFalse(offers.isEmpty());

        for (FlightOfferDto offer : offers) {
            FlightSegmentDto arrival = offer.getSegments().getLast();
            FlightSegmentDto returning = offer.getReturnSegments().getFirst();

            assertTrue(returning.getDepartureDate().startsWith(DEPARTURE));

            assertFalse(instant(returning.getDepartureDate(), returning.getOrigin(), zones)
                    .isBefore(instant(arrival.getArrivalDate(), arrival.getDestination(), zones)
                            .plus(Duration.ofHours(3))));
        }
    }

    private static List<FlightOfferDto> search(String from, String to, String back, String adults) {
        return MockFlightUtils.generateMockFlights(from, to, DEPARTURE, back, adults);
    }

    private static String signature(FlightOfferDto offer) {
        return signature(offer.getSegments()) + "|" + signature(offer.getReturnSegments());
    }

    private static String signature(List<FlightSegmentDto> segments) {
        return segments.stream().map(s -> String.join(",", s.getOrigin(), s.getDestination(),
                s.getDepartureDate(), s.getArrivalDate(), s.getCarrierCode(), s.getFlightNumber(),
                s.getAircraft(), s.getDuration())).toList().toString();
    }

    private static Map<String, ZoneId> zones() throws Exception {
        Map<String, ZoneId> zones = new HashMap<>();

        try (var reader = new BufferedReader(new InputStreamReader(
                MockFlightUtils.class.getResourceAsStream("/mock_flight_airports.tsv"),
                StandardCharsets.UTF_8))) {

            for (String line; (line = reader.readLine()) != null;) {
                if (line.startsWith("A\t")) {
                    String[] fields = line.split("\t");
                    zones.put(fields[1], ZoneId.of(fields[7]));
                }
            }
        }

        assertEquals(ZoneId.of("Asia/Jerusalem"), zones.get("TLV"));
        assertEquals(ZoneId.of("Europe/London"), zones.get("LHR"));
        assertEquals(ZoneId.of("Asia/Tokyo"), zones.get("NRT"));

        return zones;
    }

    private static Instant instant(String timestamp, String airport, Map<String, ZoneId> zones) {
        return LocalDateTime.parse(timestamp).atZone(zones.get(airport)).toInstant();
    }

    private static void checkLeg(List<FlightSegmentDto> segments, String total, Map<String, ZoneId> zones) {
        Instant first = null, previous = null;

        for (int i = 0; i < segments.size(); i++) {
            FlightSegmentDto s = segments.get(i);
            Instant departure = instant(s.getDepartureDate(), s.getOrigin(), zones);
            Instant arrival = instant(s.getArrivalDate(), s.getDestination(), zones);

            assertEquals(Duration.parse(s.getDuration()), Duration.between(departure, arrival));
            assertTrue(arrival.isAfter(departure));

            if (i == 0) {
                first = departure;
            } else {
                assertEquals(segments.get(i - 1).getDestination(), s.getOrigin());
                assertTrue(Duration.between(previous, departure).toMinutes() >= 90);
            }

            previous = arrival;
        }

        assertEquals(Duration.parse(total), Duration.between(first, previous));
    }
}