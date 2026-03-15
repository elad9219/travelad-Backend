package com.example.travelad.service;

import com.example.travelad.beans.Attraction;
import com.example.travelad.beans.AttractionCacheStatus;
import com.example.travelad.dto.AttractionDto;
import com.example.travelad.repositories.AttractionCacheStatusRepository;
import com.example.travelad.repositories.AttractionRepository;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttractionsService {

    private static final Logger logger = LoggerFactory.getLogger(AttractionsService.class);

    private final RestTemplate restTemplate;
    private final AttractionRepository attractionRepository;
    private final AttractionCacheStatusRepository cacheStatusRepository;

    @Value("${geoapify.api.key}")
    private String apiKey;

    private String geocodingUrl;
    private String placesUrl;

    public AttractionsService(RestTemplate restTemplate, AttractionRepository attractionRepository,
                              AttractionCacheStatusRepository cacheStatusRepository) {
        this.restTemplate = restTemplate;
        this.attractionRepository = attractionRepository;
        this.cacheStatusRepository = cacheStatusRepository;
    }

    @PostConstruct
    public void init() {
        geocodingUrl = "https://api.geoapify.com/v1/geocode/search";
        placesUrl = "https://api.geoapify.com/v2/places";
    }

    private String normalizeCityName(String cityName) {
        if (cityName == null) return null;
        return cityName.split("-")[0].trim().toLowerCase();
    }

    public List<Attraction> searchPlacesByCity(String cityName) {
        String normalizedCity = normalizeCityName(cityName);
        boolean cacheComplete = cacheStatusRepository.findById(normalizedCity)
                .map(AttractionCacheStatus::isComplete)
                .orElse(false);

        if (cacheComplete) {
            List<Attraction> cachedAttractions = attractionRepository.findByCityIgnoreCase(normalizedCity);
            if (cachedAttractions != null && !cachedAttractions.isEmpty()) {
                logger.info("Returning cached attractions for city: {}", normalizedCity);
                return cachedAttractions;
            } else {
                logger.warn("Cache marked as complete for {} but DB is empty. Re-fetching.", normalizedCity);
            }
        }

        logger.info("Fetching attractions from Geoapify API for city: {}", normalizedCity);
        List<AttractionDto> geoapifyPlaces = fetchPlacesFromGeoapify(normalizedCity);

        List<Attraction> attractions = geoapifyPlaces.stream()
                .map(dto -> {
                    Attraction attraction = mapToAttraction(dto);
                    attraction.setCity(normalizedCity);
                    return attraction;
                })
                .filter(a -> a.getName() != null && !a.getName().isEmpty())
                .limit(20)
                .collect(Collectors.toList());

        enrichWithImages(attractions);

        cacheStatusRepository.save(new AttractionCacheStatus(normalizedCity, false));
        asyncSaveAttractions(attractions, normalizedCity);

        return attractions;
    }

    private void enrichWithImages(List<Attraction> attractions) {
        attractions.parallelStream().forEach(attraction -> {
            String wikiImageUrl = fetchWikipediaImage(attraction.getName());
            attraction.setImageUrl(wikiImageUrl);
        });
    }

    private String fetchWikipediaImage(String attractionName) {
        try {
            String cleanName = attractionName.replaceAll("(?i),.*$", "").trim();

            boolean isLatinOnly = cleanName.matches("^[\\p{IsLatin}\\p{Punct}\\s0-9]+$");
            if (!isLatinOnly) {
                return null;
            }

            String encodedSearch = URLEncoder.encode(cleanName, StandardCharsets.UTF_8.toString());
            String wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&format=json&generator=search"
                    + "&gsrsearch=" + encodedSearch
                    + "&gsrlimit=1&prop=pageimages&pithumbsize=800";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TraveladProject/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(wikiUrl, HttpMethod.GET, entity, String.class);
            String body = response.getBody();

            if (body != null) {
                JSONObject json = new JSONObject(body);
                if (json.has("query")) {
                    JSONObject pages = json.getJSONObject("query").getJSONObject("pages");
                    String firstKey = pages.keys().next();
                    JSONObject page = pages.getJSONObject(firstKey);

                    if (page.has("thumbnail")) {
                        return page.getJSONObject("thumbnail").getString("source");
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Wikipedia fetch failed for: {}", attractionName);
        }
        return null;
    }

    @Async
    public void asyncSaveAttractions(List<Attraction> attractions, String city) {
        boolean allSaved = true;
        if (attractions != null) {
            for (Attraction attraction : attractions) {
                try {
                    Optional<Attraction> existing = attractionRepository.findByNameAndCityIgnoreCase(attraction.getName(), attraction.getCity());
                    if (existing.isEmpty()) {
                        attractionRepository.save(attraction);
                    }
                } catch (DataAccessException e) {
                    allSaved = false;
                    logger.error("Error saving attraction {}: {}", attraction.getName(), e.getMessage());
                }
            }
        }
        if (allSaved) {
            cacheStatusRepository.save(new AttractionCacheStatus(city, true));
        }
    }

    private List<AttractionDto> fetchPlacesFromGeoapify(String cityName) {
        try {
            String encodedCity = URLEncoder.encode(cityName, StandardCharsets.UTF_8.toString());
            String geocodingRequestUrl = geocodingUrl + "?text=" + encodedCity + "&apiKey=" + apiKey;

            String geocodingResponse = restTemplate.getForObject(geocodingRequestUrl, String.class);
            JSONObject geocodingJson = new JSONObject(geocodingResponse);
            JSONArray features = geocodingJson.getJSONArray("features");

            if (features.isEmpty()) {
                throw new RuntimeException("City not found in Geoapify");
            }

            // שולף את ה-place_id המדויק של העיר במקום נקודות ציון
            JSONObject properties = features.getJSONObject(0).getJSONObject("properties");
            if (!properties.has("place_id")) {
                throw new RuntimeException("place_id not found for city");
            }
            String placeId = properties.getString("place_id");

            // חיפוש לפי הגבולות של ה-place_id ולא לפי רדיוס שרירותי
            String placesRequestUrl = placesUrl + "?categories=tourism.sights&filter=place:" + placeId + "&limit=20&apiKey=" + apiKey + "&lang=en";
            String placesResponse = restTemplate.getForObject(placesRequestUrl, String.class);

            return parsePlaces(new JSONObject(placesResponse));
        } catch (Exception e) {
            logger.error("Error fetching places from Geoapify for city {}: {}", cityName, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<AttractionDto> parsePlaces(JSONObject json) {
        List<AttractionDto> list = new ArrayList<>();
        if (!json.has("features")) return list;

        JSONArray features = json.getJSONArray("features");
        for (int i = 0; i < features.length(); i++) {
            JSONObject props = features.getJSONObject(i).getJSONObject("properties");

            String name = props.optString("name:en", null);
            if (name == null || name.isEmpty()) {
                name = props.optString("name", null);
            }
            if (name == null || name.isEmpty()) continue;

            AttractionDto dto = new AttractionDto(
                    name,
                    props.optString("city", null),
                    props.optString("country", null),
                    props.optString("description", null)
            );
            dto.setAddress(props.optString("formatted", null));
            dto.setPhone(props.optJSONObject("contact") != null ? props.getJSONObject("contact").optString("phone", null) : null);
            dto.setWebsite(props.optString("website", null));
            dto.setOpening_hours(props.optString("opening_hours", null));
            list.add(dto);
        }
        return list;
    }

    private Attraction mapToAttraction(AttractionDto dto) {
        Attraction a = new Attraction();
        a.setName(dto.getName());
        a.setCity(dto.getCity());
        a.setCountry(dto.getCountry());
        a.setDescription(dto.getDescription());
        a.setAddress(dto.getAddress());
        a.setPhone(dto.getPhone());
        a.setWebsite(dto.getWebsite());
        a.setOpeningHours(dto.getOpening_hours());
        return a;
    }
}