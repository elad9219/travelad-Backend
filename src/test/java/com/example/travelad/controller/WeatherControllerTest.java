package com.example.travelad.controller;

import com.example.travelad.config.AppConfig;
import com.example.travelad.service.WeatherService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeatherController.class)
@Import({WeatherService.class, AppConfig.class})
@TestPropertySource(properties = {
        "weatherapi.api.key=weather-test-secret",
        "weatherapi.base.url=http://api.weatherapi.com/v1"
})
class WeatherControllerTest {

    private static final String VALID = "{\"location\":{\"name\":\"Paris\",\"region\":\"Ile-de-France\","
            + "\"country\":\"France\"},\"current\":{\"temp_c\":20.0,\"temp_f\":68.0,"
            + "\"wind_kph\":10.0,\"humidity\":50,\"last_updated\":\"2026-11-16 12:00\","
            + "\"condition\":{\"text\":\"Sunny\",\"icon\":\"//cdn.weatherapi.com/weather/64x64/day/113.png\"}}}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RestTemplate http;

    @Autowired
    private WeatherService service;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.bindTo(http).build();
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    private void respond(String body, HttpStatus status) {
        server.expect(request -> {
            assertEquals("https", request.getURI().getScheme());
            assertEquals("api.weatherapi.com", request.getURI().getHost());
            assertEquals("/v1/current.json", request.getURI().getPath());
        }).andRespond(
                withStatus(status)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body));
    }

    @Test
    void preservesTheExistingWeatherResponseAndUsesTheSharedHttpClient()
            throws Exception {
        respond(VALID, HttpStatus.OK);

        mvc.perform(get("/weather").param("city", "Paris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Paris"))
                .andExpect(jsonPath("$.country").value("France"))
                .andExpect(jsonPath("$.temperatureC").value(20.0))
                .andExpect(jsonPath("$.temperatureF").value(68.0))
                .andExpect(jsonPath("$.windSpeedKph").value(10.0))
                .andExpect(jsonPath("$.humidity").value(50))
                .andExpect(jsonPath("$.condition").value("Sunny"))
                .andExpect(jsonPath("$.conditionIcon")
                        .value("//cdn.weatherapi.com/weather/64x64/day/113.png"));
    }

    @Test
    void acceptsIntegerTemperaturesAndZeroReadings() throws Exception {
        String body = VALID.replace("20.0", "0")
                .replace("68.0", "32")
                .replace("10.0", "0")
                .replace("\"humidity\":50", "\"humidity\":0");

        respond(body, HttpStatus.OK);

        mvc.perform(get("/weather").param("city", "Paris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temperatureC").value(0.0))
                .andExpect(jsonPath("$.windSpeedKph").value(0.0))
                .andExpect(jsonPath("$.humidity").value(0));
    }

    @Test
    void cityIsEncodedExactlyOnceIncludingUnicodeAndReservedCharacters()
            throws Exception {
        String city = "東京 A+B%20&C";

        server.expect(request -> {
            assertEquals("https", request.getURI().getScheme());

            String query = request.getURI().getRawQuery();
            String encoded = Arrays.stream(query.split("&"))
                    .filter(value -> value.startsWith("q="))
                    .findFirst()
                    .orElseThrow()
                    .substring(2);

            assertEquals(city, URLDecoder.decode(encoded, StandardCharsets.UTF_8));
            assertEquals(2, query.split("&").length);
        }).andRespond(withSuccess(VALID, MediaType.APPLICATION_JSON));

        mvc.perform(get("/weather").param("city", "  " + city + "  "))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Paris\nprobe"})
    void invalidCitiesAreRejectedBeforeCallingTheProvider(String city)
            throws Exception {
        mvc.perform(get("/weather").param("city", city))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void missingAndOverlongCitiesAreBadRequests() throws Exception {
        mvc.perform(get("/weather"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/weather").param("city", "x".repeat(121)))
                .andExpect(status().isBadRequest());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.getWeatherByCity(" "));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    @Test
    void onlyTheDocumentedUnknownLocationErrorBecomesNotFound()
            throws Exception {
        respond(
                "{\"error\":{\"code\":1006,\"message\":\"No matching location\"}}",
                HttpStatus.BAD_REQUEST);

        mvc.perform(get("/weather").param("city", "Unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 429, 500})
    void providerFailuresAreNotMisreportedAsMissingCities(int status)
            throws Exception {
        respond(
                "{\"error\":{\"code\":2006,\"message\":\"weather-test-secret\"}}",
                HttpStatus.valueOf(status));

        mvc.perform(get("/weather").param("city", "Paris"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(content().string(
                        not(containsString("weather-test-secret"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "null",
            "{}",
            "{",
            "{\"location\":null}",
            "{\"error\":{\"code\":1006}}"
    })
    void malformedSuccessResponsesAreProviderFailures(String body)
            throws Exception {
        respond(body, HttpStatus.OK);

        mvc.perform(get("/weather").param("city", "Paris"))
                .andExpect(status().isBadGateway());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"20\"", "1e999"})
    void missingOrInvalidTemperaturesAreRejected(String temperature)
            throws Exception {
        respond(
                VALID.replace(
                        "\"temp_c\":20.0",
                        "\"temp_c\":" + temperature),
                HttpStatus.OK);

        mvc.perform(get("/weather").param("city", "Paris"))
                .andExpect(status().isBadGateway());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "101", "50.5"})
    void invalidHumidityIsRejected(String humidity) throws Exception {
        respond(
                VALID.replace(
                        "\"humidity\":50",
                        "\"humidity\":" + humidity),
                HttpStatus.OK);

        mvc.perform(get("/weather").param("city", "Paris"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void anAbsentOptionalIconDoesNotDiscardWeatherReadings()
            throws Exception {
        respond(
                VALID.replace(
                        ",\"icon\":\"//cdn.weatherapi.com/weather/64x64/day/113.png\"",
                        ""),
                HttpStatus.OK);

        mvc.perform(get("/weather").param("city", "Paris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditionIcon").value(""));
    }

    @Test
    void timeoutDoesNotReplayAutomaticallyAndANewRequestCanRecover()
            throws Exception {
        server.expect(request -> {})
                .andRespond(withException(
                        new SocketTimeoutException("weather-test-secret")));

        respond(VALID, HttpStatus.OK);

        mvc.perform(get("/weather").param("city", "Paris"))
                .andExpect(status().isBadGateway())
                .andExpect(content().string(
                        not(containsString("weather-test-secret"))));

        mvc.perform(get("/weather").param("city", "Paris"))
                .andExpect(status().isOk());
    }
}