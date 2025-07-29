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

    /**
     * Gets the list of recently searched cities for a specific user.
     */
    public List<String> getCities(String userId) {
        String key = "user_cities:" + userId;
        List<String> cities = redisTemplate.opsForList().range(key, 0, -1).stream()
                .distinct()
                .collect(Collectors.toList());
        return cities.isEmpty() ? List.of() : cities;
    }

    /**
     * Adds a city to the user's search history.
     */
    public String addCity(String userId, String city) {
        String key = "user_cities:" + userId;
        if (redisTemplate.opsForList().range(key, 0, -1).contains(city)) {
            redisTemplate.opsForList().remove(key, 1, city);
        }
        redisTemplate.opsForList().leftPush(key, city);
        // Limit the list to 10 cities to prevent unlimited growth
        redisTemplate.opsForList().trim(key, 0, 9);
        return "City '" + city + "' added to user's history successfully";
    }

    /**
     * Removes a specific city from the user's search history.
     */
    public String removeCity(String userId, String city) {
        String key = "user_cities:" + userId;
        List<String> cities = redisTemplate.opsForList().range(key, 0, -1);
        if (cities != null && cities.contains(city)) {
            redisTemplate.opsForList().remove(key, 1, city);
            return "City '" + city + "' removed from user's history successfully";
        }
        return "City '" + city + "' not found in user's history";
    }

    /**
     * Clears all cities from the user's search history.
     */
    public String clearCities(String userId) {
        String key = "user_cities:" + userId;
        redisTemplate.delete(key);
        return "All cities cleared from user's history successfully";
    }
}