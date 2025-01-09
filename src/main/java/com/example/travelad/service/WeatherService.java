package com.example.travelad.service;

import com.example.travelad.dto.WeatherDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WeatherService {
    @Value("${weatherapi.api.key}")
    private String apiKey;

    @Value("${weatherapi.base.url}")
    private String baseUrl;

    public WeatherDto getWeatherByCity(String city) {
        String url = String.format("%s/current.json?key=%s&q=%s", baseUrl, apiKey, city);
        RestTemplate restTemplate = new RestTemplate();

        try {
            var response = restTemplate.getForObject(url, Map.class);

            // Parse the response into WeatherDto
            if (response != null) {
                var location = (Map<?, ?>) response.get("location");
                var current = (Map<?, ?>) response.get("current");
                var condition = (Map<?, ?>) current.get("condition");

                WeatherDto weatherDto = new WeatherDto();
                weatherDto.setCity((String) location.get("name"));
                weatherDto.setRegion((String) location.get("region"));
                weatherDto.setCountry((String) location.get("country"));
                weatherDto.setTemperatureC((Double) current.get("temp_c"));
                weatherDto.setTemperatureF((Double) current.get("temp_f"));
                weatherDto.setCondition((String) condition.get("text"));
                weatherDto.setConditionIcon((String) condition.get("icon"));
                weatherDto.setWindSpeedKph((Double) current.get("wind_kph"));
                weatherDto.setHumidity((Integer) current.get("humidity"));
                weatherDto.setLastUpdated((String) current.get("last_updated"));

                return weatherDto;
            }
        } catch (HttpClientErrorException.BadRequest e) {
            // Log the error and return null or an error message
            System.err.println("Failed to fetch weather data for city: " + city + ". Error: " + e.getMessage());
            return null; // or you could return a WeatherDto with error status
        } catch (Exception e) {
            // For any other errors
            System.err.println("An unexpected error occurred while fetching weather data: " + e.getMessage());
            e.printStackTrace();
            return null; // or handle differently
        }
        return null;
    }
}