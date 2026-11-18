package com.example.travelad.config;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecRuntime;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GooglePlacesQuotaTest {

    private static final String PLACES = "https://maps.googleapis.com/maps/api/place/";
    private static final String NOVEMBER = "travelad:google-places:requests:2026-11";
    private static final String NOVEMBER_EXPIRY =
            Long.toString(Instant.parse("2026-12-02T08:00:00Z").getEpochSecond());

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, String> redis = mock(RedisTemplate.class);

    @Test
    void springCanCreateTheQuotaWithBothRedisTemplates() {
        try (var context = new AnnotationConfigApplicationContext(
                RedisTemplates.class, GooglePlacesQuota.class)) {
            assertNotNull(context.getBean(GooglePlacesQuota.class));
        }
    }

    @Test
    void deniesAllPlacesRequestsBeforeConnectingWhenReservationFails() throws Exception {
        var config = new AppConfig();
        try (var client = config.outboundHttpClient(quota("2026-12-01T07:59:59Z"))) {
            assertThrows(IOException.class, () -> client.execute(
                    new HttpGet(PLACES + "textsearch/json?query=Paris"), response -> response.getCode()));
            assertThrows(IOException.class, () -> client.execute(
                    new HttpGet(PLACES + "details/json?fields=photos"), response -> response.getCode()));
            assertThrows(IOException.class, () -> client.execute(
                    new HttpGet(PLACES + "photo?maxwidth=1200"), response -> response.getCode()));
        }

        verify(redis).execute(any(RedisScript.class), eq(List.of(NOVEMBER)), eq("800"), eq(NOVEMBER_EXPIRY));
        verify(redis, times(2))
                .execute(any(RedisScript.class), eq(List.of(NOVEMBER)), eq("600"), eq(NOVEMBER_EXPIRY));
    }

    @Test
    void usesTheNextMonthAtPacificMidnightAndStopsWhenRedisIsDown() throws Exception {
        var key = List.of("travelad:google-places:requests:2026-12");
        var expiry = Long.toString(Instant.parse("2027-01-02T08:00:00Z").getEpochSecond());
        when(redis.execute(any(RedisScript.class), eq(key), eq("600"), eq(expiry)))
                .thenThrow(new RedisConnectionFailureException("offline"));

        try (var client = new AppConfig().outboundHttpClient(quota("2026-12-01T08:00:00Z"))) {
            var exception = assertThrows(IOException.class, () -> client.execute(
                    new HttpGet(PLACES + "photo"), response -> response.getCode()));
            assertEquals("Places request counter unavailable", exception.getMessage());
        }
    }

    @Test
    void otherHostsBypassTheCounter() throws Exception {
        var request = new HttpGet("https://api.weatherapi.com/v1/current.json");
        var host = new HttpHost("https", "api.weatherapi.com", 443);
        var scope = new ExecChain.Scope("test", new HttpRoute(host),
                request, mock(ExecRuntime.class), HttpClientContext.create());
        var chain = mock(ExecChain.class);

        quota("2026-11-18T12:00:00Z").execute(request, scope, chain);

        verify(chain).proceed(request, scope);
        verifyNoInteractions(redis);
    }

    private GooglePlacesQuota quota(String instant) {
        return new GooglePlacesQuota(redis, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RedisTemplates {

        @Bean
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate() {
            return mock(RedisTemplate.class);
        }

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }
    }
}