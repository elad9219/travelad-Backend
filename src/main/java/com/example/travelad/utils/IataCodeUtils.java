package com.example.travelad.utils;

import com.example.travelad.beans.IataCodeEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public class IataCodeUtils {

    private static final Logger logger = LoggerFactory.getLogger(IataCodeUtils.class);
    private static final Pattern IATA_CODE = Pattern.compile("[A-Z]{3}");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final List<IataCodeEntry> entries = loadEntries();

    private static List<IataCodeEntry> loadEntries() {
        try (InputStream input = new ClassPathResource("iata_codes.json").getInputStream()) {
            return new ObjectMapper().readValue(input, new TypeReference<List<IataCodeEntry>>() {});
        } catch (IOException e) {
            logger.error("Error loading IATA code entries", e);
            return List.of();
        }
    }

    public static List<IataCodeEntry> getIataCodeEntries() {
        return entries;
    }

    public static String getIataCodeForCity(String city) {
        return resolve(city, IataCodeEntry::getCityCode);
    }

    public static String getFinalDestinationIataForCity(String city) {
        return resolve(city, entry -> {
            String airportCode = validCode(entry.getAirportCode());
            return airportCode != null ? airportCode : entry.getCityCode();
        });
    }

    private static String resolve(String input, Function<IataCodeEntry, String> selectCode) {
        String name = normalizeName(input);
        if (name.isEmpty()) {
            return null;
        }

        // The advanced search uses "Tel Aviv" while the catalog uses "Tel Aviv Yafo".
        if (name.equals("tel aviv") || name.equals("tel aviv-yafo")) {
            name = "tel aviv yafo";
        }

        String explicitCode = findCode(input);
        if (explicitCode != null && input.strip().equals(explicitCode)) {
            return explicitCode;
        }

        String lookupName = name;
        List<IataCodeEntry> exactMatches = entries.stream()
                .filter(entry -> normalizeName(entry.getFullName()).equals(lookupName))
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
                    int separator = fullName == null ? -1 : fullName.indexOf(',');
                    return separator >= 0 && normalizeName(fullName.substring(0, separator)).equals(lookupName);
                })
                .toList();
        return uniqueCode(cityMatches, IataCodeEntry::getCityCode);
    }

    private static String uniqueCode(List<IataCodeEntry> matches, Function<IataCodeEntry, String> selectCode) {
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

        return entries.stream().anyMatch(entry ->
                code.equals(validCode(entry.getCityCode())) || code.equals(validCode(entry.getAirportCode())))
                ? code : null;
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
        return WHITESPACE.matcher(name).replaceAll(" ").strip().toLowerCase(Locale.ROOT);
    }
}