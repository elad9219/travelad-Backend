package com.example.travelad.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CityCacheService {

    private static final Pattern USER_ID = Pattern.compile("[A-Za-z0-9_-]{1,128}");
    private static final long HISTORY_TTL_SECONDS = 30L * 24 * 60 * 60;

    private static final DefaultRedisScript<Long> ADD_CITY = new DefaultRedisScript<>(
            "redis.call('LREM', KEYS[1], 0, ARGV[1]); " +
                    "redis.call('LPUSH', KEYS[1], ARGV[1]); " +
                    "redis.call('LTRIM', KEYS[1], 0, 9); " +
                    "redis.call('EXPIRE', KEYS[1], ARGV[2]); " +
                    "return 1;",
            Long.class
    );

    private final RedisTemplate<String, String> redisTemplate;

    public CityCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public static String validateUserId(String userId) {
        if (userId == null || !USER_ID.matcher(userId).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid history identifier"
            );
        }
        return userId;
    }

    public static String normalizeCity(String city) {
        if (city == null || city.length() > 120 ||
                city.codePoints().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid city name"
            );
        }

        String normalized = city.strip();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "City name is required"
            );
        }
        return normalized;
    }

    private String key(String userId) {
        return "user_cities:" + validateUserId(userId);
    }

    public List<String> getCities(String userId) {
        // Reading history must remain read-only for the public availability probe.
        String historyKey = key(userId);
        List<String> cities = redisTemplate.opsForList().range(historyKey, 0, 9);

        if (cities == null) {
            return List.of();
        }

        return cities.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    public String addCity(String userId, String city) {
        String historyKey = key(userId);
        String normalizedCity = normalizeCity(city);

        // Keep deduplication, ordering, size and expiry atomic across requests.
        Long result = redisTemplate.execute(
                ADD_CITY,
                Collections.singletonList(historyKey),
                normalizedCity,
                Long.toString(HISTORY_TTL_SECONDS)
        );

        if (result == null) {
            throw new IllegalStateException("History update did not complete");
        }

        return "City '" + normalizedCity + "' added to user's history successfully";
    }

    public String removeCity(String userId, String city) {
        String historyKey = key(userId);
        String normalizedCity = normalizeCity(city);
        Long removed = redisTemplate.opsForList().remove(
                historyKey,
                0,
                normalizedCity
        );

        return "City '" + normalizedCity + (removed != null && removed > 0
                ? "' removed from user's history successfully"
                : "' not found in user's history");
    }

    public String clearCities(String userId) {
        redisTemplate.delete(key(userId));
        return "All cities cleared from user's history successfully";
    }
}