package com.example.travelad.controller;

import com.example.travelad.service.CityCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cache")
public class CityCacheController {

    @Autowired
    private CityCacheService cityCacheService;

    @GetMapping("/cities/autocomplete")
    public List<String> autocompleteCities() {
        return cityCacheService.getCities();
    }

    @PostMapping("/cities")
    public void addCity(@RequestParam String city) {
        cityCacheService.addCity(city);
    }

    @DeleteMapping("/cities")
    public void removeCity(@RequestParam String city) {
        cityCacheService.removeCity(city);
    }

    @DeleteMapping("/cities/clear")
    public void clearCities() {
        cityCacheService.clearCities();
    }
}
