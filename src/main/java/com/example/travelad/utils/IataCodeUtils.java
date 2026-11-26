package com.example.travelad.utils;

import com.example.travelad.beans.IataCodeEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

public class IataCodeUtils {

    private static final Logger logger =
            LoggerFactory.getLogger(IataCodeUtils.class);

    private static final Pattern IATA_CODE = Pattern.compile("[A-Z]{3}");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private static final Pattern WHITESPACE =
            Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private static final List<IataCodeEntry> entries = loadEntries();

    private static final Map<String, String> qualifiedCodes =
            loadQualifiedCodes();

    private static final Map<String, String> airportNames =
            loadAirportNames();

    private static final List<IataCodeEntry> displayEntries =
            buildDisplayEntries();

    private static List<IataCodeEntry> loadEntries() {
        try (InputStream input =
                     new ClassPathResource("iata_codes.json").getInputStream()) {

            return new ObjectMapper().readValue(
                    input,
                    new TypeReference<List<IataCodeEntry>>() {}
            );

        } catch (IOException e) {
            logger.error("Error loading IATA code entries", e);
            return List.of();
        }
    }

    public static List<IataCodeEntry> getIataCodeEntries() {
        return displayEntries;
    }

    private static Map<String, String> loadAirportNames() {
        Map<String, String> names = new TreeMap<>();

        try (InputStream input =
                     IataCodeUtils.class.getResourceAsStream("/flight_airport_names.tsv")) {

            if (input == null) {
                throw new IOException("Missing flight airport names");
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {

                for (String line; (line = reader.readLine()) != null;) {
                    if (line.isBlank() || line.startsWith("#")) continue;

                    String[] fields = line.split("\t", -1);

                    if (fields.length != 2
                            || !IATA_CODE.matcher(fields[0]).matches()
                            || fields[1].isBlank()
                            || names.putIfAbsent(fields[0], fields[1]) != null) {

                        throw new IOException(
                                "Invalid or duplicate flight airport name"
                        );
                    }
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not load flight airport names", e
            );
        }

        if (names.isEmpty()) {
            throw new IllegalStateException("Empty flight airport names");
        }

        return Map.copyOf(names);
    }

    private static List<IataCodeEntry> buildDisplayEntries() {
        List<IataCodeEntry> result = new ArrayList<>();
        Map<String, List<IataCodeEntry>> legacyByAirport = new HashMap<>();

        for (IataCodeEntry entry : entries) {
            String code = validCode(entry.getAirportCode());

            if (code != null && airportNames.containsKey(code)) {
                legacyByAirport
                        .computeIfAbsent(code, ignored -> new ArrayList<>())
                        .add(entry);
            } else {
                result.add(entry);
            }
        }

        // Keep city resolution separate from the physical airport display catalog.
        for (var airport : new TreeMap<>(airportNames).entrySet()) {
            IataCodeEntry entry = new IataCodeEntry();

            entry.setAirportCode(airport.getKey());
            entry.setFullName(airport.getValue());

            entry.setCityCode(uniqueCode(
                    legacyByAirport.getOrDefault(
                            airport.getKey(),
                            List.of()
                    ),
                    IataCodeEntry::getCityCode
            ));

            result.add(entry);
        }

        return List.copyOf(result);
    }

    private static Map<String, String> loadQualifiedCodes() {
        try (InputStream input =
                     new ClassPathResource("city_iata_index.json").getInputStream()) {

            JsonNode codes = new ObjectMapper().readTree(input).path("codes");

            if (!codes.isObject()) {
                throw new IOException("Invalid qualified city index");
            }

            Map<String, String> result = new HashMap<>();

            codes.fields().forEachRemaining(entry -> {
                String code = validCode(entry.getValue().asText());

                if (code != null) {
                    result.put(entry.getKey(), code);
                }
            });

            return Map.copyOf(result);

        } catch (IOException e) {
            logger.error("Error loading qualified IATA codes", e);
            return Map.of();
        }
    }

    public static String getIataCodeForCity(String city) {
        return resolve(city, IataCodeEntry::getCityCode);
    }

    public static String getFinalDestinationIataForCity(String city) {
        return resolve(city, entry -> {
            String airportCode = validCode(entry.getAirportCode());

            return airportCode != null
                    ? airportCode
                    : entry.getCityCode();
        });
    }

    private static String resolve(
            String input,
            Function<IataCodeEntry, String> selectCode
    ) {
        String name = normalizeName(input);

        if (name.isEmpty()) {
            return null;
        }

        String qualifiedCode = qualifiedCodes.get(
                name.replaceAll("\\s*,\\s*", ", ")
        );

        if (qualifiedCode != null) {
            return qualifiedCode;
        }

        // The advanced search uses "Tel Aviv" while the
        // catalog uses "Tel Aviv Yafo".
        if (name.equals("tel aviv") || name.equals("tel aviv-yafo")) {
            name = "tel aviv yafo";
        }

        String explicitCode = findCode(input);

        if (explicitCode != null && input.strip().equals(explicitCode)) {
            return explicitCode;
        }

        String lookupName = name;

        List<IataCodeEntry> exactMatches = entries.stream()
                .filter(entry ->
                        normalizeName(entry.getFullName()).equals(lookupName))
                .toList();

        if (!exactMatches.isEmpty()) {
            return uniqueCode(exactMatches, selectCode);
        }

        if (explicitCode != null) {
            return explicitCode;
        }

        List<IataCodeEntry> cityMatches = entries.stream()
                .filter(entry -> {
                    String fullName = entry.getFullName();
                    int separator = fullName == null
                            ? -1
                            : fullName.indexOf(',');

                    return separator >= 0
                            && normalizeName(
                            fullName.substring(0, separator)
                    ).equals(lookupName);
                })
                .toList();

        if (!cityMatches.isEmpty()) {
            return uniqueCode(cityMatches, IataCodeEntry::getCityCode);
        }

        List<String> airportMatches = airportNames.entrySet().stream()
                .filter(entry ->
                        normalizeName(entry.getValue()).equals(lookupName))
                .map(Map.Entry::getKey)
                .limit(2)
                .toList();

        return airportMatches.size() == 1
                ? airportMatches.getFirst()
                : null;
    }

    private static String uniqueCode(
            List<IataCodeEntry> matches,
            Function<IataCodeEntry, String> selectCode
    ) {
        List<String> codes = matches.stream()
                .map(selectCode)
                .map(IataCodeUtils::validCode)
                .filter(Objects::nonNull)
                .distinct()
                .limit(2)
                .toList();

        return codes.size() == 1 ? codes.getFirst() : null;
    }

    private static String findCode(String input) {
        String code = validCode(input);

        if (code == null) {
            return null;
        }

        return airportNames.containsKey(code)
                || entries.stream().anyMatch(entry ->
                code.equals(validCode(entry.getCityCode()))
                        || code.equals(validCode(entry.getAirportCode())))
                ? code
                : null;
    }

    private static String validCode(String input) {
        if (input == null) {
            return null;
        }

        String code = input.strip().toUpperCase(Locale.ROOT);

        return IATA_CODE.matcher(code).matches() ? code : null;
    }

    private static String normalizeName(String input) {
        if (input == null) {
            return "";
        }

        String name = Normalizer.normalize(input, Normalizer.Form.NFD);

        name = DIACRITICS.matcher(name).replaceAll("");

        return WHITESPACE.matcher(name)
                .replaceAll(" ")
                .strip()
                .toLowerCase(Locale.ROOT);
    }
}