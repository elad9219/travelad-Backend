package com.example.travelad.service;

import com.example.travelad.dto.WeatherDto;
import com.example.travelad.exceptions.ExternalApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class WeatherService {

    private final RestTemplate http;
    private final String apiKey;
    private final String baseUrl;
    private final ObjectMapper json = new ObjectMapper();

    public WeatherService(RestTemplate http,
                          @Value("${weatherapi.api.key}") String apiKey,
                          @Value("${weatherapi.base.url}") String baseUrl) {
        this.http = http;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public WeatherDto getWeatherByCity(String city) {
        String normalized = CityCacheService.normalizeCity(city);
        String body;

        try {
            body = http.getForObject(requestUri(normalized), String.class);
        } catch (RestClientResponseException failure) {
            if (failure.getRawStatusCode() == 400
                    && isUnknownLocation(failure.getResponseBodyAsString())) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Weather location not found");
            }
            throw unavailable();
        } catch (RestClientException failure) {
            throw unavailable();
        }

        if (body == null || body.isBlank()) {
            throw unavailable();
        }

        try {
            JsonNode root = json.readTree(body);
            JsonNode location = root.path("location");
            JsonNode current = root.path("current");
            JsonNode condition = current.path("condition");

            double wind = number(current, "wind_kph");
            double humidity = number(current, "humidity");

            if (wind < 0
                    || humidity < 0
                    || humidity > 100
                    || humidity != Math.rint(humidity)) {
                throw unavailable();
            }

            WeatherDto result = new WeatherDto();
            result.setCity(text(location, "name"));
            result.setRegion(optionalText(location, "region"));
            result.setCountry(text(location, "country"));
            result.setTemperatureC(number(current, "temp_c"));
            result.setTemperatureF(number(current, "temp_f"));
            result.setCondition(text(condition, "text"));
            result.setConditionIcon(optionalText(condition, "icon"));
            result.setWindSpeedKph(wind);
            result.setHumidity((int) humidity);
            result.setLastUpdated(text(current, "last_updated"));

            return result;
        } catch (JsonProcessingException failure) {
            throw unavailable();
        }
    }

    private URI requestUri(String city) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl(baseUrl);

        URI configured = URI.create(baseUrl);

        // Preserve existing configuration while securing calls to the actual provider.
        if ("api.weatherapi.com".equalsIgnoreCase(configured.getHost())
                && "http".equalsIgnoreCase(configured.getScheme())) {
            builder.scheme("https").port(-1);
        }

        return builder.pathSegment("current.json")
                .queryParam("key", "{key}")
                .queryParam("q", "{city}")
                .encode()
                .buildAndExpand(apiKey, city)
                .toUri();
    }

    private boolean isUnknownLocation(String body) {
        try {
            JsonNode root = json.readTree(body);

            if (root == null) {
                return false;
            }

            JsonNode code = root.path("error").path("code");

            return code.isIntegralNumber()
                    && code.canConvertToInt()
                    && code.intValue() == 1006;
        } catch (JsonProcessingException failure) {
            return false;
        }
    }

    private double number(JsonNode parent, String field) {
        JsonNode value = parent.path(field);

        if (!value.isNumber() || !Double.isFinite(value.doubleValue())) {
            throw unavailable();
        }

        return value.doubleValue();
    }

    private String text(JsonNode parent, String field) {
        String value = optionalText(parent, field);

        if (value.isBlank()) {
            throw unavailable();
        }

        return value;
    }

    private String optionalText(JsonNode parent, String field) {
        JsonNode value = parent.path(field);
        return value.isTextual() ? value.textValue() : "";
    }

    private ExternalApiException unavailable() {
        // Do not include upstream bodies, request URLs or API keys in exceptions.
        return new ExternalApiException("Weather provider unavailable");
    }
}