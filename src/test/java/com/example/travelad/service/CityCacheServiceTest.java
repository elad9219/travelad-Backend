package com.example.travelad.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CityCacheServiceTest {

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, String> redis = mock(RedisTemplate.class);

    @SuppressWarnings("unchecked")
    private final ListOperations<String, String> lists = mock(ListOperations.class);

    private final CityCacheService service = new CityCacheService(redis);

    @Test
    void missingHistoryIsEmptyAndReadDoesNotWrite() {
        when(redis.opsForList()).thenReturn(lists);
        when(lists.range("user_cities:portfolio_probe_123", 0, 9)).thenReturn(null);

        assertEquals(List.of(), service.getCities("portfolio_probe_123"));

        verify(redis).opsForList();
        verifyNoMoreInteractions(redis);
    }

    @Test
    void readsAreBoundedAndDeduplicateLegacyEntriesWithoutRewritingThem() {
        when(redis.opsForList()).thenReturn(lists);
        when(lists.range("user_cities:user-1", 0, 9))
                .thenReturn(List.of("東京", "Paris", "東京"));

        assertEquals(List.of("東京", "Paris"), service.getCities("user-1"));

        verify(redis).opsForList();
        verifyNoMoreInteractions(redis);
    }

    @Test
    @SuppressWarnings("unchecked")
    void addUsesOneAtomicUpdateAndAnExpiry() {
        when(redis.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenReturn(1L);

        service.addCity("user-1", " 東京 ");

        verify(redis).execute(
                any(RedisScript.class),
                eq(List.of("user_cities:user-1")),
                eq("東京"),
                eq("2592000")
        );
        verifyNoMoreInteractions(redis);
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingUpdateResultIsNotReportedAsSuccess() {
        when(redis.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () -> service.addCity("user-1", "Paris")
        );
    }

    @Test
    void removalDeletesAllLegacyDuplicatesInOneOperation() {
        when(redis.opsForList()).thenReturn(lists);
        when(lists.remove("user_cities:user-1", 0, "Paris")).thenReturn(2L);

        assertTrue(service.removeCity("user-1", " Paris ").contains("removed"));

        verify(lists).remove("user_cities:user-1", 0, "Paris");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "bad:id", "user\nother", "../other"})
    void invalidIdentifiersNeverReachRedis(String userId) {
        assertThrows(
                ResponseStatusException.class,
                () -> service.getCities(userId)
        );
        assertThrows(
                ResponseStatusException.class,
                () -> service.addCity(userId, "Paris")
        );
        assertThrows(
                ResponseStatusException.class,
                () -> service.removeCity(userId, "Paris")
        );
        assertThrows(
                ResponseStatusException.class,
                () -> service.clearCities(userId)
        );

        verifyNoInteractions(redis);
    }

    @Test
    void invalidCityNeverReachesRedis() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.addCity("user-1", " ")
        );
        assertThrows(
                ResponseStatusException.class,
                () -> service.removeCity("user-1", "a".repeat(121))
        );
        assertThrows(
                ResponseStatusException.class,
                () -> service.addCity("user-1", "Tokyo\nInjected")
        );

        verifyNoInteractions(redis);
    }
}