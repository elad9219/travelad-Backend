package com.example.travelad.controller;


import com.example.travelad.beans.WeatherDto;
import com.example.travelad.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @GetMapping("/weather")
    public WeatherDto getWeather(@RequestParam String city) {
        return weatherService.getWeatherByCity(city);
    }
}
