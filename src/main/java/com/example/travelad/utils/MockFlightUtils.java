package com.example.travelad.utils;

import com.example.travelad.dto.FlightOfferDto;
import com.example.travelad.dto.FlightSegmentDto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Offline illustrative flight offers. The network and aircraft assignments are
 * modeling assumptions, not published airline routes, schedules or availability.
 */
public final class MockFlightUtils {
    private static final String LOGO_BASE_URL = "https://pics.avs.io/200/200/";
    private static final DateTimeFormatter LOCAL_TIMESTAMP =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");
    private static final int OFFER_COUNT = 12;
    private static final double MIN_LEG_KM = 140;
    private static final Catalog CATALOG = loadCatalog();

    private record Airport(String code, double latitude, double longitude,
                           String country, String continent, String size, ZoneId zone) {}
    private record Catalog(Map<String, Airport> airports, Map<String, List<String>> metros) {}
    private record Aircraft(String code, String name, int rangeKm, int cruiseKmh) {}
    private record Carrier(String code, Set<String> hubs, List<Aircraft> fleet, double fareFactor) {}
    private record Hop(Airport from, Airport to, Carrier carrier, double km) {}
    private record Route(List<Hop> hops, double km) {}
    private record Leg(List<FlightSegmentDto> segments, Instant departure, Instant arrival) {
        String duration() {
            return Duration.between(departure, arrival).toString();
        }
    }

    private static final Aircraft A320 = new Aircraft("320", "Airbus A320", 4800, 810);
    private static final Aircraft A321 = new Aircraft("32Q", "Airbus A321neo", 5800, 815);
    private static final Aircraft B738 = new Aircraft("738", "Boeing 737-800", 4800, 810);
    private static final Aircraft B7M8 = new Aircraft("7M8", "Boeing 737 MAX 8", 5100, 815);
    private static final Aircraft B789 = new Aircraft("789", "Boeing 787-9", 13500, 880);
    private static final Aircraft B78X = new Aircraft("78X", "Boeing 787-10", 11000, 880);
    private static final Aircraft B77W = new Aircraft("77W", "Boeing 777-300ER", 13200, 870);
    private static final Aircraft A359 = new Aircraft("359", "Airbus A350-900", 14500, 880);
    private static final Aircraft A339 = new Aircraft("339", "Airbus A330-900", 11500, 870);
    private static final Aircraft E195 = new Aircraft("E95", "Embraer 195", 3000, 780);
    private static final Set<String> REGIONAL_CARRIERS = Set.of("U2", "W6", "FR", "IZ", "6H", "A3");
    private static final Set<String> NEAR_EUROPE = Set.of("IL", "CY", "TR", "EG", "MA", "TN", "JO", "GE", "AM");

    // Representative long-haul markets of the demo network, not a live route database.
    private static final Set<String> LY_LONG_HAUL = Set.of(
            "JFK", "EWR", "BOS", "MIA", "FLL", "LAX", "SFO", "BKK", "HKT", "NRT", "DEL", "BOM");

    private static final List<Carrier> CARRIERS = List.of(
            carrier("LY", 1.10, "TLV", B738, B789),
            carrier("IZ", 0.90, "TLV", A320),
            carrier("6H", 0.85, "TLV", A320),
            carrier("BA", 1.10, "LHR,LGW", A320, A321, B789, B77W),
            carrier("U2", 0.72, "LGW,LTN,BER,GVA,CDG,MXP", A320, A321),
            carrier("W6", 0.70, "BUD,WAW,LTN", A320, A321),
            carrier("FR", 0.68, "DUB,STN,BGY,MAD,BCN", B738),
            carrier("AF", 1.10, "CDG,ORY", A320, A359, B77W),
            carrier("LH", 1.10, "FRA,MUC", A320, A321, A359, B789),
            carrier("KL", 1.05, "AMS", B738, B789, B77W),
            carrier("LX", 1.10, "ZRH", A320, A321, B77W),
            carrier("OS", 1.00, "VIE", A320, E195, B789),
            carrier("IB", 1.00, "MAD", A320, A321, A359),
            carrier("TP", 0.95, "LIS", A320, A321, A339),
            carrier("AZ", 1.00, "FCO,LIN", A320, A321, A359),
            carrier("A3", 0.90, "ATH", A320, A321),
            carrier("TK", 0.95, "IST", A321, B738, B789, B77W, A359),
            carrier("EK", 1.10, "DXB", B77W, A359),
            carrier("QR", 1.10, "DOH", A320, B789, B77W, A359),
            carrier("ET", 0.95, "ADD", B738, B789, A359),
            carrier("AI", 0.95, "DEL,BOM", A320, A321, B789, B77W, A359),
            carrier("SQ", 1.15, "SIN", B78X, B77W, A359),
            carrier("CX", 1.10, "HKG", A321, B77W, A359),
            carrier("TG", 1.00, "BKK", A320, B77W, A359),
            carrier("JL", 1.10, "HND,NRT", B738, B789, A359),
            carrier("NH", 1.10, "HND,NRT", B738, B789, B77W),
            carrier("KE", 1.05, "ICN", B738, B789, B77W),
            carrier("CA", 0.95, "PEK", A320, B738, B789, A359),
            carrier("UA", 1.05, "EWR,IAD,ORD,SFO,IAH,DEN", B738, A320, B789, B77W),
            carrier("DL", 1.05, "ATL,JFK,DTW,MSP,SLC,SEA", B738, A321, A359),
            carrier("AA", 1.00, "DFW,CLT,MIA,JFK,ORD", B738, A321, B789, B77W),
            carrier("AC", 1.00, "YYZ,YUL,YVR", A320, B7M8, B789, B77W),
            carrier("QF", 1.10, "SYD,MEL,PER,BNE", B738, B789),
            carrier("NZ", 1.05, "AKL", A320, A321, B789, B77W),
            carrier("LA", 0.95, "SCL,LIM,GRU", A320, A321, B789)
    );

    private static final Carrier REGIONAL_DEMO =
            carrier("DEMO", 0.90, "", E195, A320, B738);

    private static final List<Airport> HUBS = CARRIERS.stream()
            .flatMap(c -> c.hubs.stream()).distinct().sorted()
            .map(CATALOG.airports::get).filter(Objects::nonNull).toList();

    private MockFlightUtils() {}

    public static List<FlightOfferDto> generateMockFlights(
            String origin, String destination, String departDate,
            String returnDate, String adults) {
        LocalDate departure = LocalDate.parse(departDate);
        LocalDate returning = returnDate == null || returnDate.isBlank()
                ? null : LocalDate.parse(returnDate);
        int passengers = Integer.parseInt(adults);
        List<Airport> origins = resolve(origin);
        List<Airport> destinations = resolve(destination);
        if (origins.isEmpty() || destinations.isEmpty()) return List.of();
        if (origins.stream().anyMatch(destinations::contains)) return List.of();

        List<Route> direct = new ArrayList<>();
        List<Route> connecting = new ArrayList<>();
        for (Airport from : origins) {
            for (Airport to : destinations) {
                double distance = distance(from, to);
                if (distance < MIN_LEG_KM) continue;
                for (Carrier c : carriersFor(from, to, distance)) {
                    direct.add(new Route(List.of(new Hop(from, to, c, distance)), distance));
                }
                for (Airport hub : HUBS) {
                    if (origins.contains(hub) || destinations.contains(hub)) continue;
                    double first = distance(from, hub);
                    double second = distance(hub, to);
                    double total = first + second;
                    if (first < MIN_LEG_KM || second < MIN_LEG_KM
                            || total > Math.min(distance * 1.65, distance + 4000)) continue;
                    List<Carrier> a = carriersFor(from, hub, first);
                    List<Carrier> b = carriersFor(hub, to, second);
                    boolean throughCarrier = a.stream().anyMatch(b::contains);
                    for (Carrier c : a) {
                        for (Carrier d : b) {
                            if (throughCarrier && !c.equals(d)) continue;
                            connecting.add(new Route(List.of(
                                    new Hop(from, hub, c, first),
                                    new Hop(hub, to, d, second)), total));
                        }
                    }
                }
            }
        }

        // Prefer reasonable connections instead of sampling very long detours.
        connecting.sort(Comparator.comparingDouble(Route::km));
        if (connecting.size() > 120) connecting = new ArrayList<>(connecting.subList(0, 120));
        if (direct.isEmpty() && connecting.isEmpty()) return List.of();

        long seed = Objects.hash("travelad-flight-v2", origin, destination, departure, returning);
        Random random = new Random(seed);
        List<FlightOfferDto> offers = new ArrayList<>();
        Set<String> identities = new HashSet<>();

        for (int attempt = 0; attempt < 96 && offers.size() < OFFER_COUNT; attempt++) {
            List<Route> pool = !direct.isEmpty() && (connecting.isEmpty() || attempt % 3 != 2)
                    ? direct : connecting;
            Route route = pool.get(random.nextInt(pool.size()));
            Leg outbound = schedule(route, departure, null, random);
            if (outbound == null) continue;

            Leg inbound = null;
            if (returning != null) {
                inbound = schedule(reverse(route), returning,
                        outbound.arrival.plus(Duration.ofHours(3)), random);
                if (inbound == null) continue;
            }
            String key = identity(outbound) + "|" + (inbound == null ? "" : identity(inbound));
            if (!identities.add(key)) continue;

            double base = fare(route, departure, random);
            if (inbound != null) base += fare(reverse(route), returning, random);
            FlightOfferDto offer = new FlightOfferDto();
            offer.setId("MOCK-" + Long.toUnsignedString(seed, 36) + "-" + offers.size());
            offer.setSource("MOCK");
            offer.setSegments(outbound.segments);
            offer.setOutboundSegments(outbound.segments);
            offer.setOutboundDuration(outbound.duration());
            if (inbound != null) {
                offer.setReturnSegments(inbound.segments);
                offer.setReturnDuration(inbound.duration());
            }
            // Round once per person so different passenger counts scale exactly.
            offer.setPrice(Math.round(base) * (double) passengers);
            offers.add(offer);
        }

        offers.sort(Comparator.comparingDouble(FlightOfferDto::getPrice));
        return offers;
    }

    private static List<Carrier> carriersFor(Airport from, Airport to, double km) {
        List<Carrier> carriers = new ArrayList<>();
        if (km < MIN_LEG_KM) return carriers;

        for (Carrier carrier : CARRIERS) {
            if (!carrier.hubs.contains(from.code) && !carrier.hubs.contains(to.code)) continue;
            if (from.size.equals("S") || to.size.equals("S")) continue;
            if (REGIONAL_CARRIERS.contains(carrier.code)
                    && (km > 4500 || !nearEurope(from) || !nearEurope(to))) continue;

            Airport other = carrier.hubs.contains(from.code) ? to : from;
            if ("LY".equals(carrier.code) && !nearEurope(other)
                    && !LY_LONG_HAUL.contains(other.code)) continue;

            if (carrier.fleet.stream().anyMatch(aircraft -> aircraftFits(aircraft, from, to, km))) {
                carriers.add(carrier);
            }
        }

        // No real route is asserted for unsupported regional combinations.
        if (carriers.isEmpty() && km <= 2600
                && (!from.size.equals("S") && !to.size.equals("S") || km <= 1200)
                && (from.country.equals(to.country) || from.continent.equals(to.continent)
                || km <= 1200)) {
            carriers.add(REGIONAL_DEMO);
        }

        return carriers;
    }

    private static boolean nearEurope(Airport airport) {
        return "EU".equals(airport.continent) || NEAR_EUROPE.contains(airport.country);
    }

    private static boolean aircraftFits(Aircraft aircraft, Airport from, Airport to, double km) {
        return km <= aircraft.rangeKm && (aircraft.rangeKm < 6000
                || from.size.equals("L") && to.size.equals("L"));
    }

    private static Leg schedule(Route route, LocalDate date, Instant earliest, Random random) {
        Airport first = route.hops.getFirst().from;
        ZonedDateTime departure = null;
        int start = 5 * 60 + random.nextInt(19 * 12) * 5;

        // Try every five-minute slot in the requested local date.
        for (int i = 0; i < 19 * 12; i++) {
            int minute = 300 + (start - 300 + i * 5) % (19 * 60);
            ZonedDateTime candidate = ZonedDateTime.of(date, LocalTime.of(minute / 60, minute % 60), first.zone);
            if (!candidate.toLocalDate().equals(date)) continue;
            if (earliest == null || !candidate.toInstant().isBefore(earliest)) {
                departure = candidate;
                break;
            }
        }

        if (departure == null) return null;
        Instant startInstant = departure.toInstant();
        Instant nextDeparture = startInstant;
        List<FlightSegmentDto> segments = new ArrayList<>();
        Instant arrival = startInstant;

        for (Hop hop : route.hops) {
            List<Aircraft> fleet = hop.carrier.fleet.stream()
                    .filter(aircraft -> aircraftFits(aircraft, hop.from, hop.to, hop.km))
                    .filter(aircraft -> hop.km >= 3000 || aircraft.rangeKm < 6000
                            || hop.carrier.fleet.stream().noneMatch(a -> a.rangeKm < 6000))
                    .toList();

            Aircraft aircraft = fleet.get(random.nextInt(fleet.size()));
            long minutes = Math.max(40, Math.round(30 + hop.km / aircraft.cruiseKmh * 60
                    * (0.97 + random.nextDouble() * 0.12)));

            arrival = nextDeparture.plus(Duration.ofMinutes(minutes));

            ZonedDateTime localDeparture = nextDeparture.atZone(hop.from.zone);
            ZonedDateTime localArrival = arrival.atZone(hop.to.zone);

            // The existing DTO uses offset-free local timestamps. Do not emit ambiguous times.
            if (hop.from.zone.getRules().getValidOffsets(localDeparture.toLocalDateTime()).size() != 1
                    || hop.to.zone.getRules().getValidOffsets(localArrival.toLocalDateTime()).size() != 1) {
                return null;
            }

            String carrier = hop.carrier.code;
            segments.add(new FlightSegmentDto(
                    hop.from.code, hop.to.code,
                    localDeparture.format(LOCAL_TIMESTAMP),
                    localArrival.format(LOCAL_TIMESTAMP),
                    Duration.ofMinutes(minutes).toString(), carrier,
                    String.valueOf(100 + random.nextInt(8900)),
                    aircraft.code, aircraft.name, null, null,
                    carrier.equals("DEMO") ? null : LOGO_BASE_URL + carrier + ".png"
            ));
            nextDeparture = arrival.plus(Duration.ofMinutes(90 + random.nextInt(25) * 5));
        }

        return new Leg(List.copyOf(segments), startInstant, arrival);
    }

    private static Route reverse(Route route) {
        List<Hop> reversed = new ArrayList<>();
        for (Hop hop : route.hops.reversed()) {
            reversed.add(new Hop(hop.to, hop.from, hop.carrier, hop.km));
        }
        return new Route(List.copyOf(reversed), route.km);
    }

    private static double fare(Route route, LocalDate date, Random random) {
        double price = 0;
        for (Hop hop : route.hops) price += (35 + hop.km * 0.055) * hop.carrier.fareFactor;

        double season = switch (date.getMonth()) {
            case JULY, AUGUST, DECEMBER -> 1.16;
            default -> 1.0;
        };

        double weekend = switch (date.getDayOfWeek()) {
            case FRIDAY, SUNDAY -> 1.08;
            default -> 1.0;
        };

        double connection = route.hops.size() > 1 ? 0.85 : 1;
        return price * season * weekend * connection * (0.85 + random.nextDouble() * 0.65);
    }

    private static String identity(Leg leg) {
        return leg.segments.stream()
                .map(s -> s.getOrigin() + "-" + s.getDestination() + "-" +
                        s.getDepartureDate() + "-" + s.getCarrierCode())
                .reduce("", (a, b) -> a + "|" + b);
    }

    private static Carrier carrier(String code, double factor, String hubs, Aircraft... fleet) {
        return new Carrier(code, hubs.isEmpty() ? Set.of() : Set.of(hubs.split(",")), List.of(fleet), factor);
    }

    private static List<Airport> resolve(String code) {
        Airport airport = CATALOG.airports.get(code);
        if (airport != null) return List.of(airport);
        return CATALOG.metros.getOrDefault(code, List.of()).stream()
                .map(CATALOG.airports::get).filter(Objects::nonNull).toList();
    }

    private static double distance(Airport a, Airport b) {
        double latA = Math.toRadians(a.latitude), latB = Math.toRadians(b.latitude);
        double dLat = latB - latA;
        double dLon = Math.toRadians(b.longitude - a.longitude);
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(latA) * Math.cos(latB) * Math.pow(Math.sin(dLon / 2), 2);
        return 6371 * 2 * Math.asin(Math.sqrt(Math.min(1, h)));
    }

    private static Catalog loadCatalog() {
        Map<String, Airport> airports = new HashMap<>();
        Map<String, List<String>> metros = new HashMap<>();

        try (InputStream input = MockFlightUtils.class.getResourceAsStream("/mock_flight_airports.tsv")) {
            if (input == null) throw new IOException("Missing mock flight airport catalog");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#") || line.isBlank()) continue;
                    String[] f = line.split("\t");

                    if (f.length == 8 && f[0].equals("A")) {
                        airports.put(f[1], new Airport(f[1], Double.parseDouble(f[2]),
                                Double.parseDouble(f[3]), f[4], f[5], f[6], ZoneId.of(f[7])));
                    } else if (f.length == 3 && f[0].equals("M")) {
                        metros.put(f[1], List.of(f[2].split(",")));
                    } else {
                        throw new IOException("Invalid mock flight airport row");
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Could not load mock flight airport catalog", e);
        }

        if (airports.size() < 1000 || !airports.containsKey("TLV")) {
            throw new IllegalStateException("Incomplete mock flight airport catalog");
        }

        return new Catalog(Map.copyOf(airports), Map.copyOf(metros));
    }
}