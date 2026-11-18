package com.example.travelad.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicRequestLimitsTest {

    private final AtomicLong clock = new AtomicLong();
    private final Object handler = new Object();

    @Test
    void exhaustedBurstReturns429AndRefillsIncrementally() {
        var gate = new PublicRequestLimits.RequestGate(2, 2, 2, true, clock::get);
        var request = new MockHttpServletRequest("GET", "/api/places/search");
        var response = new MockHttpServletResponse();

        for (int i = 0; i < 2; i++) {
            assertTrue(gate.preHandle(request, response, handler));
            gate.afterCompletion(request, response, handler, null);
        }

        assertFalse(gate.preHandle(request, response, handler));
        assertEquals(429, response.getStatus());
        assertEquals("2", response.getHeader("Retry-After"));
        assertEquals("no-store", response.getHeader("Cache-Control"));

        clock.set(TimeUnit.SECONDS.toNanos(1));
        assertFalse(gate.preHandle(request, response, handler));
        assertEquals("1", response.getHeader("Retry-After"));

        clock.set(TimeUnit.SECONDS.toNanos(2));
        assertTrue(gate.preHandle(request, response, handler));
        gate.afterCompletion(request, response, handler, null);
        assertFalse(gate.preHandle(request, response, handler));
    }

    @Test
    void capsConcurrentWorkAndReleasesTheSlotAfterAHandlerFailure() {
        var gate = new PublicRequestLimits.RequestGate(2, 1, 1, true, clock::get);
        var get = new MockHttpServletRequest("GET", "/api/places/search");
        var head = new MockHttpServletRequest("HEAD", "/hotels/by-city-name");
        var response = new MockHttpServletResponse();

        assertTrue(gate.preHandle(get, response, handler));
        assertFalse(gate.preHandle(head, response, handler));
        assertEquals("1", response.getHeader("Retry-After"));

        gate.afterCompletion(get, response, handler, new IllegalStateException());
        assertTrue(gate.preHandle(head, new MockHttpServletResponse(), handler));
    }

    @Test
    void historyReadsDeletesAndPreflightsDoNotSpendTheWriteBudget() {
        var gate = new PublicRequestLimits.RequestGate(1, 1, 1, false, clock::get);
        var post = new MockHttpServletRequest("POST", "/cache/cities");
        var response = new MockHttpServletResponse();

        assertTrue(gate.preHandle(post, response, handler));
        gate.afterCompletion(post, response, handler, null);
        for (String method : new String[]{"GET", "DELETE", "OPTIONS"}) {
            var request = new MockHttpServletRequest(method, "/cache/cities");
            assertTrue(gate.preHandle(request, response, handler));
            gate.afterCompletion(request, response, handler, null);
        }
        var alias = new MockHttpServletRequest("POST", "/api/places/cache/cities");
        assertFalse(gate.preHandle(alias, response, handler));
    }
}