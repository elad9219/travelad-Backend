package com.example.travelad.config;

import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Component
public class GooglePlacesQuota implements ExecChainHandler {

    private static final ZoneId BILLING_ZONE = ZoneId.of("America/Los_Angeles");
    private static final int MONTHLY_LIMIT = 800;
    private static final int PHOTO_CUTOFF = 600;
    private static final String PLACES_PATH = "/maps/api/place/";
    private static final String CITY_SEARCH_PATH = PLACES_PATH + "textsearch/json";

    private static final DefaultRedisScript<Long> RESERVE = new DefaultRedisScript<>("""
            local used = tonumber(redis.call('GET', KEYS[1]) or '0')
            if used >= tonumber(ARGV[1]) then
                return 0
            end
            redis.call('INCR', KEYS[1])
            redis.call('EXPIREAT', KEYS[1], ARGV[2])
            return 1
            """, Long.class);

    private final RedisTemplate<String, String> redis;
    private final Clock clock;

    @Autowired
    public GooglePlacesQuota(@Qualifier("redisTemplate") RedisTemplate<String, String> redis) {
        this(redis, Clock.system(BILLING_ZONE));
    }

    GooglePlacesQuota(RedisTemplate<String, String> redis, Clock clock) {
        this.redis = redis;
        this.clock = clock.withZone(BILLING_ZONE);
    }

    @Override
    public ClassicHttpResponse execute(
            ClassicHttpRequest request, ExecChain.Scope scope, ExecChain chain
    ) throws IOException, HttpException {
        String host = scope.route.getTargetHost().getHostName();
        String path = request.getPath();

        if ("maps.googleapis.com".equalsIgnoreCase(host) && path.startsWith(PLACES_PATH)) {
            boolean citySearch = path.equals(CITY_SEARCH_PATH) || path.startsWith(CITY_SEARCH_PATH + "?");
            reserve(citySearch ? MONTHLY_LIMIT : PHOTO_CUTOFF);
        }

        return chain.proceed(request, scope);
    }

    private void reserve(int limit) throws IOException {
        YearMonth month = YearMonth.now(clock);
        String key = "travelad:google-places:requests:" + month;
        // Keep the previous month's key briefly for requests already in flight at rollover.
        long expiresAt = month.plusMonths(1).atDay(2).atStartOfDay(BILLING_ZONE).toEpochSecond();
        Long reserved;

        try {
            reserved = redis.execute(RESERVE, List.of(key), Integer.toString(limit), Long.toString(expiresAt));
        } catch (RuntimeException e) {
            // A counter failure must stop the outgoing request.
            throw new IOException("Places request counter unavailable");
        }

        if (!Long.valueOf(1L).equals(reserved)) {
            throw new IOException("Places request not reserved");
        }
    }
}