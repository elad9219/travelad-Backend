package com.example.travelad.service;

import com.example.travelad.beans.Attraction;
import com.example.travelad.beans.AttractionCacheStatus;
import com.example.travelad.repositories.AttractionCacheStatusRepository;
import com.example.travelad.repositories.AttractionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttractionsService {
    static final int VERSION = 3;
    private static final Logger log = LoggerFactory.getLogger(AttractionsService.class);
    private final AttractionRepository attractions;
    private final AttractionCacheStatusRepository statuses;
    private final AttractionImageService images;
    private final RestTemplate http;
    private final String apiKey;

    @Autowired
    public AttractionsService(AttractionRepository attractions, AttractionCacheStatusRepository statuses,
                              AttractionImageService images, @Value("${geoapify.api.key}") String apiKey) {
        this(attractions, statuses, images, apiKey, AttractionImageService.client());
    }

    AttractionsService(AttractionRepository attractions, AttractionCacheStatusRepository statuses,
                       AttractionImageService images, String apiKey, RestTemplate http) {
        this.attractions = attractions;
        this.statuses = statuses;
        this.images = images;
        this.apiKey = apiKey;
        this.http = http;
    }

    // Serialize refreshes to bound provider concurrency and avoid duplicate city inserts.
    public synchronized List<Attraction> searchPlacesByCity(String cityName) {
        String city = normalizeCity(cityName);
        long now = System.currentTimeMillis();
        AttractionCacheStatus status = statuses.findById(city).orElse(new AttractionCacheStatus(city, false));
        List<Attraction> existing = attractions.findByCityIgnoreCase(city);
        int latest = existing.stream().map(Attraction::getSelectionVersion).filter(Objects::nonNull)
                .max(Integer::compareTo).orElse(VERSION);
        List<Attraction> fallback = ordered(existing.stream()
                .filter(a -> Integer.valueOf(latest).equals(a.getSelectionVersion())).collect(Collectors.toList()));
        List<Attraction> current = ordered(existing.stream()
                .filter(a -> Integer.valueOf(VERSION).equals(a.getSelectionVersion())).collect(Collectors.toList()));
        long ttl = status.isComplete() ? 86400000L : 900000L;
        if (Integer.valueOf(VERSION).equals(status.getVersion()) && status.getRefreshedAt() != null
                && now - status.getRefreshedAt() < ttl) {
            if (current.isEmpty() && !status.isComplete()) {
                if (!fallback.isEmpty()) return fallback;
                throw new IllegalStateException("Attractions temporarily unavailable");
            }
            return current;
        }

        try {
            JsonNode geocoding = get("https://api.geoapify.com/v1/geocode/search",
                    Map.of("text", city, "type", "city", "limit", "1", "lang", "en"));
            JsonNode location = geocoding.path("features").path(0).path("properties");
            if (!location.hasNonNull("place_id") || !coordinates(location)) {
                throw new IllegalStateException("City could not be located");
            }
            Map<Attraction, JsonNode> properties = new LinkedHashMap<>();
            boolean discoveryComplete = true;
            String[] categories = {"tourism.sights,tourism.attraction", "entertainment.museum", "leisure.park"};
            int[] limits = {200, 100, 60};
            for (int i = 0; i < categories.length; i++) {
                try {
                    JsonNode result = get("https://api.geoapify.com/v2/places", Map.of(
                            "categories", categories[i], "filter", "place:" + location.path("place_id").asText(),
                            "limit", String.valueOf(limits[i]), "lang", "en"));
                    if (!result.path("features").isArray()) throw new IllegalStateException("Invalid places response");
                    for (JsonNode feature : result.path("features")) {
                        JsonNode props = feature.path("properties");
                        String name = first(props.path("name:en").asText(),
                                props.path("name_other").path("en").asText(),
                                props.path("datasource").path("raw").path("name:en").asText(), props.path("name").asText());
                        if (name.isEmpty() || !coordinates(props) || score(props, 0) == 0) continue;
                        Attraction attraction = new Attraction();
                        attraction.setName(name);
                        attraction.setCity(city);
                        attraction.setCountry(props.path("country").asText());
                        attraction.setAddress(props.path("formatted").asText());
                        attraction.setDescription(props.path("description").asText());
                        attraction.setPhone(props.path("contact").path("phone").asText());
                        attraction.setWebsite(props.path("website").asText());
                        attraction.setOpeningHours(props.path("opening_hours").asText());
                        attraction.setLatitude(props.path("lat").asDouble());
                        attraction.setLongitude(props.path("lon").asDouble());
                        properties.put(attraction, props);
                    }
                } catch (Exception ex) { discoveryComplete = false; }
            }
            if (!discoveryComplete && !fallback.isEmpty()) throw new IllegalStateException("Incomplete discovery");
            if (!discoveryComplete && properties.isEmpty()) throw new IllegalStateException("Discovery failed");
            List<Attraction> candidates = new ArrayList<>(properties.keySet());
            candidates.sort(Comparator.comparingDouble((Attraction a) -> score(properties.get(a), 0)
                            + (AttractionImageService.reference(properties.get(a), "wikidata").isEmpty() ? 0 : 15)).reversed()
                    .thenComparing(Attraction::getName));
            Set<String> seen = new HashSet<>();
            candidates = candidates.stream().filter(a -> {
                JsonNode props = properties.get(a);
                String id = AttractionImageService.reference(props, "wikidata");
                return seen.add(id.matches("Q[1-9][0-9]*") ? id
                        : AttractionImageService.normalize(a.getName()) + ":"
                        + Math.round(a.getLatitude() * 10000) + ":" + Math.round(a.getLongitude() * 10000));
            }).limit(100).collect(Collectors.toList());
            Map<Attraction, String> originalNames = new IdentityHashMap<>();
            candidates.forEach(a -> originalNames.put(a, a.getName()));
            AttractionImageService.Result enrichment = candidates.isEmpty()
                    ? new AttractionImageService.Result()
                    : images.enrich(candidates, properties, location.path("country_code").asText().toLowerCase(Locale.ROOT));
            candidates.sort(Comparator.comparingDouble((Attraction a) ->
                            score(properties.get(a), enrichment.sitelinks.getOrDefault(a, 0))).reversed()
                    .thenComparing(a -> a.getImageUrl() == null).thenComparing(Attraction::getName));
            List<Attraction> selected = candidates.stream().limit(20).collect(Collectors.toList());
            if (!enrichment.complete && selected.stream().noneMatch(a -> a.getImageUrl() != null)
                    && fallback.stream().anyMatch(a -> a.getImageUrl() != null)) {
                throw new IllegalStateException("Image enrichment unavailable");
            }
            Set<Long> retained = new HashSet<>();
            for (int i = 0; i < selected.size(); i++) {
                Attraction attraction = selected.get(i);
                Optional<Attraction> old = existing.stream().filter(a -> !retained.contains(a.getId())
                        && (Objects.equals(a.getName(), attraction.getName()) || Objects.equals(a.getName(), originalNames.get(attraction)))
                        && a.getLatitude() != null && a.getLongitude() != null
                        && AttractionImageService.distance(a.getLatitude(), a.getLongitude(),
                        attraction.getLatitude(), attraction.getLongitude()) < 500).findFirst();
                old.ifPresent(a -> { attraction.setId(a.getId()); retained.add(a.getId()); });
                attraction.setSelectionVersion(VERSION);
                attraction.setDisplayOrder(i);
            }
            List<Attraction> updates = new ArrayList<>(selected);
            for (Attraction old : existing) {
                if (old.getSelectionVersion() != null && !retained.contains(old.getId())) {
                    old.setSelectionVersion(null);
                    updates.add(old);
                }
            }
            // One repository transaction stores the new selection and retires the previous one.
            attractions.saveAll(updates);
            status.setComplete(discoveryComplete && enrichment.complete);
            status.setVersion(VERSION);
            status.setRefreshedAt(System.currentTimeMillis());
            statuses.save(status);
            log.info("Attractions refreshed: city={}, discovered={}, evaluated={}, selected={}, images={}, complete={}",
                    city, properties.size(), candidates.size(), selected.size(),
                    selected.stream().filter(a -> a.getImageUrl() != null).count(), status.isComplete());
            return selected;
        } catch (Exception ex) {
            // Store a cooldown without provider URLs, exceptions or API keys.
            status.setComplete(false);
            status.setVersion(VERSION);
            status.setRefreshedAt(System.currentTimeMillis());
            statuses.save(status);
            if (!fallback.isEmpty()) return fallback;
            throw new IllegalStateException("Attractions temporarily unavailable");
        }
    }

    static double score(JsonNode props, int sitelinks) {
        double category = 0;
        for (JsonNode node : props.path("categories")) {
            String value = node.asText();
            if (value.equals("tourism.sights.tower") || value.equals("tourism.sights.castle")) category = Math.max(category, 60);
            else if (value.startsWith("entertainment.museum")) category = Math.max(category, 45);
            else if (value.startsWith("tourism.sights.place_of_worship") || value.equals("tourism.sights.archaeological_site")) category = Math.max(category, 40);
            else if (value.startsWith("leisure.park")) category = Math.max(category, 30);
            else if (value.startsWith("tourism.sights.memorial") || value.startsWith("tourism.attraction.artwork")) category = Math.max(category, 5);
            else if (value.startsWith("tourism.sights.") || value.startsWith("tourism.attraction.")) category = Math.max(category, 25);
        }
        if (category == 0 && (hasCategory(props, "tourism.sights") || hasCategory(props, "tourism.attraction"))) category = 25;
        return category == 0 ? 0 : category + 16 * Math.log1p(Math.max(0, sitelinks));
    }

    private static boolean hasCategory(JsonNode props, String category) {
        for (JsonNode value : props.path("categories")) if (category.equals(value.asText())) return true;
        return false;
    }

    static List<Attraction> ordered(List<Attraction> values) {
        values.sort(Comparator.comparing(Attraction::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(a -> a.getImageUrl() == null)
                .thenComparing(a -> a.getName() == null ? "" : a.getName()));
        return values;
    }

    private static boolean coordinates(JsonNode props) {
        return props.path("lat").isNumber() && props.path("lon").isNumber()
                && Double.isFinite(props.path("lat").asDouble()) && Math.abs(props.path("lat").asDouble()) <= 90
                && Double.isFinite(props.path("lon").asDouble()) && Math.abs(props.path("lon").asDouble()) <= 180;
    }

    private static String first(String... values) {
        for (String value : values) if (!value.trim().isEmpty()) return value.trim();
        return "";
    }

    private JsonNode get(String url, Map<String, String> parameters) throws Exception {
        UriComponentsBuilder request = UriComponentsBuilder.fromHttpUrl(url).queryParam("apiKey", apiKey);
        parameters.forEach(request::queryParam);
        String body = http.getForObject(request.build().encode().toUri(), String.class);
        JsonNode result = new ObjectMapper().readTree(body == null ? "{}" : body);
        if (result.has("error")) throw new IllegalStateException("Attractions provider unavailable");
        return result;
    }

    static String normalizeCity(String value) {
        if (value == null || value.trim().isEmpty() || value.length() > 120
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Enter a city name of up to 120 characters");
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}