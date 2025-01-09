package com.example.travelad.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CityCacheService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String CITY_KEY = "city_names";

    // Get all cities in the cache
    public Object getCities() {
        List<String> cities = redisTemplate.opsForList().range(CITY_KEY, 0, -1).stream()
                .distinct()  // Removes duplicates
                .collect(Collectors.toList()); // Collect into a list

        if (cities == null || cities.isEmpty()) {
            return "No cities in the list"; // Custom message when no cities are found
        }
        return cities; // Return the list of cities
    }

    public String addCity(String city) {
        if (redisTemplate.opsForList().range(CITY_KEY, 0, -1).contains(city)) {
            // Remove existing city before adding to ensure it's at the top
            redisTemplate.opsForList().remove(CITY_KEY, 1, city);
        }
        redisTemplate.opsForList().leftPush(CITY_KEY, city);
        return "City '" + city + "' added or updated successfully";
    }

    // Remove a specific city from the cache
    public String removeCity(String city) {
        List<String> cities = redisTemplate.opsForList().range(CITY_KEY, 0, -1);
        if (cities != null && cities.contains(city)) {
            redisTemplate.opsForList().remove(CITY_KEY, 1, city); // Remove the city from the cache
            return "City '" + city + "' removed successfully";
        }
        return "City '" + city + "' not found in the cache";
    }

    // Clear all cities from the cache and return a success message
    public String clearCities() {
        redisTemplate.delete(CITY_KEY); // Remove the key and all associated values
        return "All cities cleared successfully";
    }
}
