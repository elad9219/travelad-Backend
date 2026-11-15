package com.example.travelad.service;

import com.example.travelad.beans.Attraction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttractionImageService {
    private final RestTemplate http;
    private final ObjectMapper json = new ObjectMapper();

    public AttractionImageService() {
        this(client());
    }

    AttractionImageService(RestTemplate http) {
        this.http = http;
    }

    static RestTemplate client() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2500);
        factory.setReadTimeout(10000);

        RestTemplate client = new RestTemplate(factory);
        client.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set(
                    "User-Agent",
                    "TraveladDemo/3.0 (https://github.com/elad9219/travelad-Backend)"
            );
            return execution.execute(request, body);
        });

        return client;
    }

    static class Result {
        boolean complete = true;
        final Map<Attraction, Integer> sitelinks = new IdentityHashMap<>();
    }

    JsonNode get(UriComponentsBuilder request, long deadline) throws Exception {
        if (System.currentTimeMillis() >= deadline) {
            throw new IllegalStateException("Image lookup budget exhausted");
        }

        String body = http.getForObject(request.build().encode().toUri(), String.class);
        JsonNode result = json.readTree(body == null ? "{}" : body);

        if (result.has("error")) {
            throw new IllegalStateException("Image provider unavailable");
        }

        return result;
    }

    // At most 100 places, three title queries, two entity batches and two file batches.
    Result enrich(
            List<Attraction> places,
            Map<Attraction, JsonNode> properties,
            String countryCode
    ) {
        Result result = new Result();
        long deadline = System.currentTimeMillis() + 45000;

        Map<Attraction, String> ids = new LinkedHashMap<>();
        Map<Attraction, List<String>> files = new LinkedHashMap<>();
        Map<Attraction, String> articleTitles = new LinkedHashMap<>();

        for (Attraction place : places) {
            String id = reference(properties.get(place), "wikidata");

            if (id.matches("Q[1-9][0-9]*")) {
                ids.put(place, id);
            }

            files.put(place, new ArrayList<>());
        }

        String local = Map.of(
                "fr", "fr",
                "jp", "ja",
                "it", "it",
                "de", "de",
                "es", "es",
                "pt", "pt",
                "il", "he"
        ).get(countryCode);

        Set<String> languages = new LinkedHashSet<>(Collections.singleton("en"));
        if (local != null) {
            languages.add(local);
        }

        for (String language : languages) {
            Map<Attraction, String> titles = new LinkedHashMap<>();

            for (Attraction place : places) {
                if (ids.containsKey(place)) {
                    continue;
                }

                JsonNode props = properties.get(place);
                String wiki = reference(props, "wikipedia");
                String name = "en".equals(language)
                        ? place.getName()
                        : props.path("datasource")
                        .path("raw")
                        .path("name")
                        .asText(props.path("name").asText());

                if (wiki.startsWith(language + ":")) {
                    name = wiki.substring(language.length() + 1);
                }

                if (!name.trim().isEmpty() && !name.contains("|")) {
                    titles.put(place, name);
                }
            }

            List<String> names = titles.values().stream()
                    .distinct()
                    .limit(50)
                    .collect(Collectors.toList());

            for (int start = 0; start < names.size(); start += 50) {
                try {
                    String batch = String.join(
                            "|",
                            names.subList(start, Math.min(start + 50, names.size()))
                    );

                    UriComponentsBuilder request = UriComponentsBuilder
                            .fromHttpUrl("https://" + language + ".wikipedia.org/w/api.php")
                            .queryParam("action", "query")
                            .queryParam("format", "json")
                            .queryParam("titles", batch)
                            .queryParam("redirects", 1)
                            .queryParam("prop", "coordinates|pageprops|pageimages")
                            .queryParam("colimit", 50)
                            .queryParam("ppprop", "wikibase_item")
                            .queryParam("piprop", "name")
                            .queryParam("pilimit", 50)
                            .queryParam("pilicense", "free");

                    JsonNode query = get(request, deadline).path("query");

                    if (!query.path("pages").isObject()) {
                        throw new IllegalStateException("Invalid page response");
                    }

                    Map<String, String> redirects = new HashMap<>();

                    for (String field : List.of("normalized", "redirects")) {
                        query.path(field).forEach(link -> redirects.put(
                                link.path("from").asText(),
                                link.path("to").asText()
                        ));
                    }

                    for (Map.Entry<Attraction, String> entry : titles.entrySet()) {
                        String title = entry.getValue();

                        for (int step = 0; step < 5 && redirects.containsKey(title); step++) {
                            title = redirects.get(title);
                        }

                        for (JsonNode page : query.path("pages")) {
                            if (!title.equals(page.path("title").asText())
                                    || !near(entry.getKey(), page.path("coordinates").path(0), 500)) {
                                continue;
                            }

                            String id = page.path("pageprops").path("wikibase_item").asText();
                            if (id.matches("Q[1-9][0-9]*")) {
                                ids.put(entry.getKey(), id);
                            }

                            addFile(files.get(entry.getKey()), page.path("pageimage").asText());
                        }
                    }
                } catch (Exception ex) {
                    result.complete = false;
                }
            }
        }

        List<String> uniqueIds = new ArrayList<>(new LinkedHashSet<>(ids.values()));

        for (int start = 0; start < uniqueIds.size(); start += 50) {
            try {
                String batch = String.join(
                        "|",
                        uniqueIds.subList(start, Math.min(start + 50, uniqueIds.size()))
                );

                UriComponentsBuilder request = UriComponentsBuilder
                        .fromHttpUrl("https://www.wikidata.org/w/api.php")
                        .queryParam("action", "wbgetentities")
                        .queryParam("format", "json")
                        .queryParam("ids", batch)
                        .queryParam("props", "claims|sitelinks|labels|descriptions")
                        .queryParam("languages", "en");

                JsonNode entities = get(request, deadline).path("entities");

                if (!entities.isObject()) {
                    throw new IllegalStateException("Invalid entity response");
                }

                for (Map.Entry<Attraction, String> entry : ids.entrySet()) {
                    JsonNode entity = entities.path(entry.getValue());
                    if (entity.isMissingNode()) {
                        continue;
                    }

                    Attraction place = entry.getKey();
                    boolean nearby = false;

                    for (JsonNode claim : entity.path("claims").path("P625")) {
                        JsonNode coordinate = claim.path("mainsnak")
                                .path("datavalue")
                                .path("value");

                        if (!"deprecated".equals(claim.path("rank").asText())
                                && coordinate.path("globe").asText().endsWith("/Q2")
                                && near(place, coordinate, 1500)) {
                            nearby = true;
                        }
                    }

                    if (!nearby) {
                        continue;
                    }

                    int count = 0;
                    Iterator<String> sites = entity.path("sitelinks").fieldNames();

                    while (sites.hasNext()) {
                        String site = sites.next();

                        if (site.endsWith("wiki")
                                && !site.equals("commonswiki")
                                && !site.equals("specieswiki")) {
                            count++;
                        }
                    }

                    result.sitelinks.put(place, count);

                    String articleTitle = entity.path("sitelinks")
                            .path("enwiki")
                            .path("title")
                            .asText();

                    if (!articleTitle.isEmpty() && !articleTitle.contains("|")) {
                        articleTitles.put(place, articleTitle);
                    }

                    List<JsonNode> claims = new ArrayList<>();
                    entity.path("claims").path("P18").forEach(claims::add);
                    claims.sort(Comparator.comparing(
                            claim -> !"preferred".equals(claim.path("rank").asText())
                    ));

                    List<String> choices = new ArrayList<>();

                    for (JsonNode claim : claims) {
                        if (!"deprecated".equals(claim.path("rank").asText())) {
                            addFile(
                                    choices,
                                    claim.path("mainsnak").path("datavalue").path("value").asText()
                            );
                        }
                    }

                    choices.addAll(files.get(place));
                    files.put(
                            place,
                            choices.stream().distinct().limit(2).collect(Collectors.toList())
                    );

                    String english = entity.path("labels").path("en").path("value").asText();
                    if (!english.isEmpty()) {
                        place.setName(english);
                    }

                    if (place.getDescription() == null || place.getDescription().isEmpty()) {
                        place.setDescription(
                                entity.path("descriptions").path("en").path("value").asText()
                        );
                    }
                }
            } catch (Exception ex) {
                result.complete = false;
            }
        }

        List<Attraction> imageCandidates = new ArrayList<>(places);
        imageCandidates.sort(Comparator.comparingDouble(
                (Attraction place) -> AttractionsService.score(
                        properties.get(place),
                        result.sitelinks.getOrDefault(place, 0)
                )
        ).reversed());

        imageCandidates = imageCandidates.stream().limit(40).collect(Collectors.toList());

        List<Attraction> missingFiles = imageCandidates.stream()
                .filter(place -> files.get(place).isEmpty() && articleTitles.containsKey(place))
                .collect(Collectors.toList());

        if (!missingFiles.isEmpty()) {
            try {
                String names = missingFiles.stream()
                        .map(articleTitles::get)
                        .distinct()
                        .collect(Collectors.joining("|"));

                UriComponentsBuilder request = UriComponentsBuilder
                        .fromHttpUrl("https://en.wikipedia.org/w/api.php")
                        .queryParam("action", "query")
                        .queryParam("format", "json")
                        .queryParam("titles", names)
                        .queryParam("redirects", 1)
                        .queryParam("prop", "pageimages|pageprops")
                        .queryParam("ppprop", "wikibase_item")
                        .queryParam("piprop", "name")
                        .queryParam("pilimit", 50)
                        .queryParam("pilicense", "free");

                JsonNode pages = get(request, deadline).path("query").path("pages");

                if (!pages.isObject()) {
                    throw new IllegalStateException("Invalid linked article response");
                }

                for (Attraction place : missingFiles) {
                    for (JsonNode page : pages) {
                        // The entity's location was verified before accepting its sitelink.
                        if (ids.get(place).equals(
                                page.path("pageprops").path("wikibase_item").asText()
                        )) {
                            addFile(files.get(place), page.path("pageimage").asText());
                        }
                    }
                }
            } catch (Exception ex) {
                result.complete = false;
            }
        }

        List<String> titles = imageCandidates.stream()
                .flatMap(place -> files.get(place).stream())
                .distinct()
                .collect(Collectors.toList());

        Map<String, JsonNode> infoByTitle = new HashMap<>();

        for (int start = 0; start < titles.size(); start += 50) {
            try {
                String batch = String.join(
                        "|",
                        titles.subList(start, Math.min(start + 50, titles.size()))
                );

                UriComponentsBuilder request = UriComponentsBuilder
                        .fromHttpUrl("https://commons.wikimedia.org/w/api.php")
                        .queryParam("action", "query")
                        .queryParam("format", "json")
                        .queryParam("redirects", 1)
                        .queryParam("titles", batch)
                        .queryParam("prop", "imageinfo")
                        .queryParam("iiurlwidth", 640)
                        .queryParam("iiprop", "url|extmetadata|mime|size");

                JsonNode query = get(request, deadline).path("query");

                if (!query.path("pages").isObject()) {
                    throw new IllegalStateException("Invalid file response");
                }

                query.path("pages").forEach(page -> infoByTitle.put(
                        fileKey(page.path("title").asText()),
                        page.path("imageinfo").path(0)
                ));

                for (String field : List.of("redirects", "normalized")) {
                    query.path(field).forEach(link -> {
                        JsonNode info = infoByTitle.get(fileKey(link.path("to").asText()));

                        if (info != null) {
                            infoByTitle.put(fileKey(link.path("from").asText()), info);
                        }
                    });
                }
            } catch (Exception ex) {
                result.complete = false;
            }
        }

        for (Attraction place : imageCandidates) {
            for (String title : files.get(place)) {
                JsonNode info = infoByTitle.get(fileKey(title));

                if (info != null && applyImage(place, info)) {
                    break;
                }
            }
        }

        return result;
    }

    static String reference(JsonNode props, String key) {
        if (props == null) {
            return "";
        }

        String value = props.path("wiki_and_media").path(key).asText();
        return value.isEmpty() ? props.path("datasource").path("raw").path(key).asText() : value;
    }

    private static void addFile(List<String> files, String name) {
        if (!name.isEmpty() && !name.contains("|")) {
            files.add("File:" + name.replaceFirst("^File:", ""));
        }
    }

    private static String fileKey(String value) {
        return value.replace('_', ' ').trim();
    }

    static boolean applyImage(Attraction place, JsonNode info) {
        JsonNode metadata = info.path("extmetadata");
        String license = metadata.path("LicenseShortName").path("value").asText();
        String source = info.path("descriptionurl").asText();
        String image = info.path("thumburl").asText(info.path("url").asText());
        String mime = info.path("mime").asText();

        if (!license.matches("CC BY(?:-SA)?(?: [0-9.]+)?|CC0(?: [0-9.]+)?|Public domain")
                || !Set.of("image/jpeg", "image/png", "image/webp").contains(mime)
                || info.path("width").asInt() < 320
                || info.path("height").asInt() < 180
                || !safeUrl(source, "commons.wikimedia.org")
                || !(safeUrl(image, "upload.wikimedia.org")
                || safeUrl(image, "thumb.wikimedia.org"))
                || !URI.create(image).getPath().startsWith("/wikipedia/commons/")) {
            return false;
        }

        String artist = HtmlUtils.htmlUnescape(
                metadata.path("Artist").path("value").asText().replaceAll("<[^>]*>", "")
        ).replaceAll("\\s+", " ").trim();

        if (license.startsWith("CC BY") && artist.isEmpty()) {
            return false;
        }

        place.setImageUrl(image);
        place.setImagePageUrl(source);
        place.setImageAttribution(artist.isEmpty() ? "Wikimedia Commons" : artist);
        place.setImageLicense(license);

        return true;
    }

    static boolean near(Attraction place, JsonNode coordinate, double maximum) {
        double lat = coordinate.path("lat")
                .asDouble(coordinate.path("latitude").asDouble(Double.NaN));
        double lon = coordinate.path("lon")
                .asDouble(coordinate.path("longitude").asDouble(Double.NaN));

        return place.getLatitude() != null
                && place.getLongitude() != null
                && Double.isFinite(lat)
                && Math.abs(lat) <= 90
                && Double.isFinite(lon)
                && Math.abs(lon) <= 180
                && distance(place.getLatitude(), place.getLongitude(), lat, lon) <= maximum;
    }

    static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    static boolean safeUrl(String value, String host) {
        try {
            URI uri = URI.create(value);

            return "https".equals(uri.getScheme())
                    && host.equals(uri.getHost())
                    && uri.getRawUserInfo() == null
                    && uri.getPort() == -1;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    static double distance(double lat1, double lon1, double lat2, double lon2) {
        double a = Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(Math.toRadians(lon2 - lon1) / 2), 2);

        return 6371000 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
    }
}