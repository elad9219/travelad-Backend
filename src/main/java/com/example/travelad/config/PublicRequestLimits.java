package com.example.travelad.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

@Configuration(proxyBeanMethods = false)
public class PublicRequestLimits implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // The hotel list can also call Google Places to resolve a city.
        registry.addInterceptor(new RequestGate(20, 1, 8, true, System::nanoTime))
                .addPathPatterns("/api/places/search", "/hotels/by-city-name");
        registry.addInterceptor(new RequestGate(16, 1, 6, true, System::nanoTime))
                .addPathPatterns("/weather");
        registry.addInterceptor(new RequestGate(8, 2, 3, true, System::nanoTime))
                .addPathPatterns("/api/geoapify/places");
        registry.addInterceptor(new RequestGate(16, 1, 4, false, System::nanoTime))
                .addPathPatterns("/cache/cities", "/api/places/cache/cities");
    }

    // One token per refill interval, with separate limits on burst and in-flight work.
    static final class RequestGate implements HandlerInterceptor {

        private final int capacity;
        private final long refillInterval;
        private final int maxInFlight;
        private final boolean read;
        private final LongSupplier clock;

        private int tokens;
        private int inFlight;
        private long lastRefill;

        RequestGate(int capacity, int refillSeconds, int maxInFlight, boolean read,
                    LongSupplier clock) {
            this.capacity = capacity;
            this.refillInterval = TimeUnit.SECONDS.toNanos(refillSeconds);
            this.maxInFlight = maxInFlight;
            this.read = read;
            this.clock = clock;
            this.tokens = capacity;
            this.lastRefill = clock.getAsLong();
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) {
            if (!limited(request)) {
                return true;
            }
            long retryAfter = acquire();
            if (retryAfter == 0) {
                return true;
            }
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            return false;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception exception) {
            // Spring calls this only after a successful preHandle.
            if (limited(request)) {
                release();
            }
        }

        private boolean limited(HttpServletRequest request) {
            String method = request.getMethod();
            return read ? "GET".equals(method) || "HEAD".equals(method) : "POST".equals(method);
        }

        private synchronized long acquire() {
            long now = clock.getAsLong();
            long elapsed = now - lastRefill;
            long refilled = elapsed / refillInterval;
            if (refilled > 0) {
                tokens += (int) Math.min(capacity - tokens, refilled);
                lastRefill = tokens == capacity ? now : lastRefill + refilled * refillInterval;
            }

            if (tokens == 0) {
                long remaining = refillInterval - (now - lastRefill);
                return TimeUnit.NANOSECONDS.toSeconds(remaining - 1) + 1;
            }
            if (inFlight >= maxInFlight) {
                return 1;
            }

            tokens--;
            inFlight++;
            return 0;
        }

        private synchronized void release() {
            inFlight--;
        }
    }
}