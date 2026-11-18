package com.example.travelad.service;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.repositories.GooglePlacesRepository;
import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GooglePlacesService {

    private static final Logger logger =
            LoggerFactory.getLogger(GooglePlacesService.class);

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final int MAX_STORED_AGE_DAYS = 29;

    private final RestTemplate http;
    private final GooglePlacesRepository repository;
    private final String apiKey;
    private final GooglePhotoUrlResolver photoResolver;
    private final Object[] locks = new Object[32];

    private final Map<String, CachedPlace> cache = Collections.synchronizedMap(
            new LinkedHashMap<String, CachedPlace>(100, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, CachedPlace> eldest) {
                    return size() > 100;
                }
            }
    );

    public GooglePlacesService(
            RestTemplate http,
            GooglePlacesRepository repository,
            GooglePhotoUrlResolver photoResolver,
            @Value("${google.places.api.key}") String apiKey) {
        this.http = http;
        this.repository = repository;
        this.apiKey = apiKey;
        this.photoResolver = photoResolver;

        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
    }

    public GooglePlaces searchPlaceByCity(String city) {
        GooglePlaces place = findPlaceByCity(city);

        if (usable(place)) {
            loadDisplayDetails(place);
        }

        return place;
    }

    public GooglePlaces findPlaceByCity(String city) {
        String requestedCity = CityCacheService.normalizeCity(city);
        String key = requestedCity.toLowerCase(Locale.ROOT);

        synchronized (locks[Math.floorMod(key.hashCode(), locks.length)]) {
            CachedPlace cached = cache.get(key);

            if (cached != null && cached.expiresAt > System.currentTimeMillis() &&
                    isFresh(cached.place)) {
                return forRequestedCity(cached.place, requestedCity);
            }

            try {
                GooglePlaces place = storedPlace(key);

                if (place != null) {
                    cache.put(key, new CachedPlace(place));
                    return forRequestedCity(place, requestedCity);
                }

                place = fetchPlace(requestedCity, key);
                cache.put(key, new CachedPlace(copyCoordinates(place)));
                return place;
            } catch (Exception e) {
                logger.warn(
                        "Place lookup failed ({})",
                        e.getClass().getSimpleName()
                );
                return fallback(requestedCity);
            }
        }
    }

    private GooglePlaces storedPlace(String city) {
        try {
            List<GooglePlaces> values = repository.findByCityIgnoreCase(city);

            if (values != null) {
                for (GooglePlaces value : values) {
                    if (usable(value) && isFresh(value)) {
                        return copyCoordinates(value);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn(
                    "City cache read failed ({})",
                    e.getClass().getSimpleName()
            );
        }

        return null;
    }

    private boolean isFresh(GooglePlaces place) {
        LocalDateTime fetchedAt = place.getCreatedAt();
        LocalDateTime now = LocalDateTime.now();
        return fetchedAt != null && !fetchedAt.isAfter(now) &&
                fetchedAt.isAfter(now.minusDays(MAX_STORED_AGE_DAYS));
    }

    private GooglePlaces fetchPlace(String city, String key) {
        JsonNode response = http.getForObject(
                url("textsearch/json", Map.of("query", city)),
                JsonNode.class
        );

        JsonNode result = response == null
                ? null
                : response.path("results").path(0);

        if (response == null ||
                !"OK".equals(response.path("status").asText()) ||
                result == null ||
                !result.isObject()) {
            throw new IllegalStateException(
                    "Place provider returned no usable result"
            );
        }

        JsonNode location = result.path("geometry").path("location");

        GooglePlaces place = new GooglePlaces();
        place.setPlaceId(result.path("place_id").asText(""));
        place.setName(result.path("name").asText(""));
        place.setCity(key);
        place.setAddress(result.path("formatted_address").asText(city));
        place.setLatitude(
                location.path("lat").isNumber()
                        ? location.path("lat").asDouble()
                        : null
        );
        place.setLongitude(
                location.path("lng").isNumber()
                        ? location.path("lng").asDouble()
                        : null
        );
        place.setCountry(country(result.path("formatted_address").asText("")));
        place.setComplete(true);
        place.setIcon("");

        if (!usable(place) || place.getName().isBlank()) {
            throw new IllegalStateException("Invalid place provider result");
        }

        try {
            repository.findByPlaceId(place.getPlaceId())
                    .ifPresent(existing -> place.setId(existing.getId()));
            repository.save(copyCoordinates(place));
        } catch (Exception e) {
            // Persistence is optional when the provider has already returned valid data.
            logger.warn(
                    "City cache write failed ({})",
                    e.getClass().getSimpleName()
            );
        }

        return place;
    }

    private void loadDisplayDetails(GooglePlaces place) {
        try {
            JsonNode response = http.getForObject(
                    url(
                            "details/json",
                            Map.of(
                                    "place_id", place.getPlaceId(),
                                    "fields", "name,formatted_address,photos"
                            )
                    ),
                    JsonNode.class
            );

            if (response == null ||
                    !"OK".equals(response.path("status").asText())) {
                return;
            }

            JsonNode result = response.path("result");
            String name = result.path("name").asText("");
            if (!name.isBlank()) {
                place.setName(name);
            }
            String address = result.path("formatted_address").asText("");
            if (!address.isBlank()) {
                place.setAddress(address);
                place.setCountry(country(address));
            }

            JsonNode photo = result.path("photos").path(0);
            String reference = photo.path("photo_reference").asText("");

            if (reference.isBlank()) {
                return;
            }

            List<String> attributions = new ArrayList<>();
            JsonNode credits = photo.path("html_attributions");

            if (credits.isArray()) {
                for (JsonNode credit : credits) {
                    if (!credit.isTextual() ||
                            credit.asText().length() > 8192 ||
                            attributions.size() >= 20) {
                        return;
                    }

                    attributions.add(credit.asText());
                }
            } else if (!credits.isMissingNode() && !credits.isNull()) {
                return;
            }

            String image = photoResolver.resolve(url(
                    "photo",
                    Map.of(
                            "maxwidth", "1200",
                            "photo_reference", reference
                    )
            ));

            if (!image.isBlank()) {
                place.setIcon(image);
                place.setPhotoAttributions(List.copyOf(attributions));
            }
        } catch (Exception e) {
            logger.warn(
                    "Place display details unavailable ({})",
                    e.getClass().getSimpleName()
            );
        }
    }

    private URI url(String path, Map<String, String> parameters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                "https://maps.googleapis.com/maps/api/place/" + path
        );

        Map<String, String> values = new LinkedHashMap<>(parameters);
        values.put("key", apiKey);

        values.forEach((name, value) ->
                builder.queryParam(name, "{" + name + "}")
        );

        return builder.encode().buildAndExpand(values).toUri();
    }

    private boolean usable(GooglePlaces place) {
        return place != null &&
                place.getPlaceId() != null &&
                !place.getPlaceId().isBlank() &&
                !place.getPlaceId().startsWith("unknown_") &&
                place.getLatitude() != null &&
                Double.isFinite(place.getLatitude()) &&
                Math.abs(place.getLatitude()) <= 90 &&
                place.getLongitude() != null &&
                Double.isFinite(place.getLongitude()) &&
                Math.abs(place.getLongitude()) <= 180;
    }

    private GooglePlaces forRequestedCity(GooglePlaces source, String city) {
        GooglePlaces result = copyCoordinates(source);
        result.setName(city);
        result.setAddress(city);
        result.setCountry("");
        return result;
    }

    private GooglePlaces copyCoordinates(GooglePlaces source) {
        GooglePlaces result = new GooglePlaces();
        result.setId(source.getId());
        result.setPlaceId(source.getPlaceId());
        result.setCity(source.getCity());
        result.setLatitude(source.getLatitude());
        result.setLongitude(source.getLongitude());
        result.setComplete(source.isComplete());
        result.setAttractionCount(source.getAttractionCount());
        result.setCreatedAt(source.getCreatedAt());
        result.setIcon("");
        return result;
    }

    private String country(String address) {
        if (address == null) {
            return "";
        }

        String[] parts = address.split(",");
        return parts.length == 0 ? "" : parts[parts.length - 1].trim();
    }

    private GooglePlaces fallback(String city) {
        GooglePlaces place = new GooglePlaces();
        place.setPlaceId("unknown_" + city);
        place.setCity(city);
        place.setName(city);
        place.setAddress(city);
        place.setLatitude(0.0);
        place.setLongitude(0.0);
        place.setCountry("");
        place.setIcon("");
        place.setComplete(false);
        return place;
    }

    private static class CachedPlace {

        private final GooglePlaces place;
        private final long expiresAt =
                System.currentTimeMillis() + CACHE_TTL_MS;

        private CachedPlace(GooglePlaces place) {
            this.place = place;
        }
    }
}