package com.example.travelad.utils;

import com.example.travelad.dto.HotelDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HotelCatalog {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Photo(
            String file,
            String author,
            String license,
            String licenseUrl,
            String sourceUrl,
            String modifications
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Entry(
            String id,
            String name,
            Double latitude,
            Double longitude,
            String localImage,
            Photo photo
    ) {}

    // Exact alternate labels from the city search index.
    // Never match a city by its first name alone.
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("London, England, United Kingdom", "London, United Kingdom"),
            Map.entry("Paris, Ile-de-France, France", "Paris, France"),
            Map.entry("New York City, New York, United States", "New York City, United States"),
            Map.entry("New York, United States", "New York City, United States"),
            Map.entry("New York, New York, United States", "New York City, United States"),
            Map.entry("Cape Town, Western Cape, South Africa", "Cape Town, South Africa"),
            Map.entry("Reykjavík, Capital Region, Iceland", "Reykjavík, Iceland"),
            Map.entry("Rome, Lazio, Italy", "Rome, Italy"),
            Map.entry("Roma, Italy", "Rome, Italy"),
            Map.entry("Roma, Lazio, Italy", "Rome, Italy"),
            Map.entry("Barcelona, Catalonia, Spain", "Barcelona, Spain"),
            Map.entry("Berlin, State of Berlin, Germany", "Berlin, Germany"),
            Map.entry("Amsterdam, The Netherlands", "Amsterdam, Netherlands"),
            Map.entry("Amsterdam, North Holland, The Netherlands", "Amsterdam, Netherlands"),
            Map.entry("Amsterdam, North Holland, Netherlands", "Amsterdam, Netherlands"),
            Map.entry("Wien, Austria", "Vienna, Austria"),
            Map.entry("Praha, Czechia", "Prague, Czechia"),
            Map.entry("Athens, Attica, Greece", "Athens, Greece"),
            Map.entry("Hong Kong, China", "Hong Kong, Hong Kong"),
            Map.entry("Sydney, New South Wales, Australia", "Sydney, Australia"),
            Map.entry("Melbourne, Victoria, Australia", "Melbourne, Australia"),
            Map.entry("Los Angeles, California, United States", "Los Angeles, United States"),
            Map.entry("San Francisco, California, United States", "San Francisco, United States"),
            Map.entry("Toronto, Ontario, Canada", "Toronto, Canada"),
            Map.entry("Ciudad de México, Mexico", "Mexico City, Mexico"),
            Map.entry("Buenos Aires, Buenos Aires F.D., Argentina", "Buenos Aires, Argentina")
    );

    private static final Map<String, List<Entry>> CITIES = load();

    private HotelCatalog() {}

    private static Map<String, List<Entry>> load() {
        try (InputStream input =
                     new ClassPathResource("hotel_catalog.json").getInputStream()) {

            Map<String, List<Entry>> raw = new ObjectMapper().readValue(
                    input,
                    new TypeReference<Map<String, List<Entry>>>() {}
            );

            Map<String, List<Entry>> cities = new HashMap<>();
            Set<String> ids = new HashSet<>();

            raw.forEach((city, entries) -> {
                if (entries.isEmpty() || cities.containsKey(normalize(city))) {
                    throw new IllegalStateException(
                            "Invalid hotel catalog city: " + city
                    );
                }

                for (Entry entry : entries) {
                    Photo photo = entry.photo();

                    if (entry.id() == null
                            || !entry.id().matches("Q[1-9][0-9]*")
                            || !ids.add(entry.id())
                            || !hasText(entry.name())
                            || entry.latitude() == null
                            || !Double.isFinite(entry.latitude())
                            || Math.abs(entry.latitude()) > 90
                            || entry.longitude() == null
                            || !Double.isFinite(entry.longitude())
                            || Math.abs(entry.longitude()) > 180
                            || !("images/" + entry.id() + ".webp").equals(entry.localImage())
                            || photo == null
                            || !hasText(photo.file())
                            || !hasText(photo.author())
                            || !hasText(photo.license())
                            || !hasText(photo.licenseUrl())
                            || !hasText(photo.sourceUrl())
                            || !hasText(photo.modifications())) {

                        throw new IllegalStateException(
                                "Invalid hotel catalog entry in " + city
                        );
                    }
                }

                cities.put(normalize(city), List.copyOf(entries));
            });

            ALIASES.forEach((alias, canonical) -> {
                List<Entry> entries = cities.get(normalize(canonical));

                if (entries == null) {
                    throw new IllegalStateException(
                            "Missing catalog city: " + canonical
                    );
                }

                List<Entry> previous = cities.putIfAbsent(
                        normalize(alias), entries
                );

                if (previous != null && previous != entries) {
                    throw new IllegalStateException(
                            "Conflicting hotel catalog alias: " + alias
                    );
                }
            });

            return Map.copyOf(cities);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to load the packaged hotel catalog", e
            );
        }
    }

    public static List<HotelDto> hotelsFor(String city) {
        return CITIES.getOrDefault(normalize(city), List.of())
                .stream()
                .map(HotelCatalog::toHotel)
                .toList();
    }

    private static HotelDto toHotel(Entry entry) {
        // A deterministic demo rate, not a live quote or market estimate.
        double price = 80 + Math.floorMod(entry.id().hashCode(), 350);

        HotelDto hotel = new HotelDto(
                entry.name(),
                "CATALOG_" + entry.id(),
                null,
                null,
                entry.latitude(),
                entry.longitude(),
                price,
                "/hotel-images/" + entry.id() + ".webp",
                null
        );

        Photo photo = entry.photo();

        hotel.setPhotoAttribution(new HotelDto.PhotoAttribution(
                photo.file(),
                photo.author(),
                photo.license(),
                photo.licenseUrl(),
                photo.sourceUrl(),
                photo.modifications()
        ));

        return hotel;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("(?U)\\s+", " ")
                .replaceAll("\\s*,\\s*", ", ")
                .strip()
                .toLowerCase(Locale.ROOT);
    }
}