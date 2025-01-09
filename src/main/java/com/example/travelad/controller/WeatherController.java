package com.example.travelad.controller;

import com.example.travelad.dto.WeatherDto;
import com.example.travelad.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @GetMapping("/weather")
    public ResponseEntity<?> getWeather(@RequestParam String city) {
        WeatherDto weather = weatherService.getWeatherByCity(city);
        if (weather == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Weather data not found for city: " + city);
        }
        return ResponseEntity.ok(weather);
    }
}