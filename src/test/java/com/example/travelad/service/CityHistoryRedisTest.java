package com.example.travelad.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "TRAVELAD_REDIS_TEST_PORT", matches = "[0-9]+")
class CityHistoryRedisTest {

    @Test
    void realRedisKeepsConcurrentHistoryBoundedAndIsolatedWithExpiry() throws Exception {
        int port = Integer.parseInt(System.getenv("TRAVELAD_REDIS_TEST_PORT"));

        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration("127.0.0.1", port);

        LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(3))
                .shutdownTimeout(Duration.ZERO)
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, client);
        factory.afterPropertiesSet();

        RedisTemplate<String, String> redis = new RedisTemplate<>();
        redis.setConnectionFactory(factory);
        redis.setKeySerializer(new StringRedisSerializer());
        redis.setValueSerializer(new StringRedisSerializer());
        redis.afterPropertiesSet();

        CityCacheService service = new CityCacheService(redis);
        String first = "test_" + UUID.randomUUID();
        String second = "test_" + UUID.randomUUID();
        String firstKey = "user_cities:" + first;
        String secondKey = "user_cities:" + second;
        ExecutorService workers = Executors.newFixedThreadPool(8);

        try {
            assertEquals(List.of(), service.getCities(first));
            assertEquals(Boolean.FALSE, redis.hasKey(firstKey));

            List<Callable<String>> updates = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                String city = "City " + (i % 16);
                updates.add(() -> service.addCity(first, city));
            }

            for (Future<String> update : workers.invokeAll(updates)) {
                update.get();
            }

            service.addCity(first, "東京+100%");

            List<String> cities = service.getCities(first);
            assertEquals(10, cities.size());
            assertEquals(10, new HashSet<>(cities).size());
            assertEquals("東京+100%", cities.get(0));

            Long ttl = redis.getExpire(firstKey, TimeUnit.SECONDS);
            assertNotNull(ttl);
            assertTrue(
                    ttl > 29L * 24 * 60 * 60 &&
                            ttl <= 30L * 24 * 60 * 60
            );

            redis.expire(firstKey, Duration.ofSeconds(90));
            service.getCities(first);

            Long ttlAfterRead = redis.getExpire(firstKey, TimeUnit.SECONDS);
            assertNotNull(ttlAfterRead);
            assertTrue(ttlAfterRead > 0 && ttlAfterRead <= 90);

            service.addCity(second, "Paris");
            service.removeCity(first, "東京+100%");

            assertFalse(service.getCities(first).contains("東京+100%"));

            service.clearCities(first);

            assertEquals(List.of(), service.getCities(first));
            assertEquals(List.of("Paris"), service.getCities(second));
        } finally {
            workers.shutdownNow();

            try {
                // Delete only the two unique keys created by this test.
                redis.delete(List.of(firstKey, secondKey));
            } finally {
                factory.destroy();
            }
        }
    }
}