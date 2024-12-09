package com.example.travelad.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CityCacheService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String CITY_KEY = "city_names";


    public List<String> getCities() {
        return redisTemplate.opsForList().range(CITY_KEY, 0, -1); // Retrieve all city names
    }

    public void addCity(String city) {
        redisTemplate.opsForList().leftPush(CITY_KEY, city); // Add the city to the start of the list
    }

    public void removeCity(String city) {
        redisTemplate.opsForList().remove(CITY_KEY, 1, city); // Remove a single occurrence of the city
    }

    public void clearCities() {
        redisTemplate.delete(CITY_KEY); // Remove the key and all associated values
    }
}
