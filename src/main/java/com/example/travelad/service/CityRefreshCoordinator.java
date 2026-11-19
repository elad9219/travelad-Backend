package com.example.travelad.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

final class CityRefreshCoordinator<T> {

    private static final long WAIT_SECONDS = 120;

    private final ConcurrentHashMap<String, CompletableFuture<T>> inFlight =
            new ConcurrentHashMap<>();

    private final Semaphore refreshSlots;

    CityRefreshCoordinator(int maxRefreshes) {
        refreshSlots = new Semaphore(maxRefreshes);
    }

    T load(String city, Supplier<T> operation) {
        CompletableFuture<T> result = new CompletableFuture<>();
        CompletableFuture<T> existing = inFlight.putIfAbsent(city, result);

        if (existing != null) {
            return await(existing);
        }

        try {
            T value = operation.get();
            result.complete(value);
            return value;
        } catch (RuntimeException | Error failure) {
            result.completeExceptionally(failure);
            throw failure;
        } finally {
            inFlight.remove(city, result);
        }
    }

    T refresh(Supplier<T> operation, Supplier<T> whenBusy) {
        if (!refreshSlots.tryAcquire()) {
            return whenBusy.get();
        }

        try {
            return operation.get();
        } finally {
            refreshSlots.release();
        }
    }

    private T await(CompletableFuture<T> result) {
        try {
            return result.get(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Attractions request interrupted");
        } catch (TimeoutException failure) {
            throw new IllegalStateException("Attractions refresh still in progress");
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();

            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }

            if (cause instanceof Error error) {
                throw error;
            }

            throw new IllegalStateException("Attractions temporarily unavailable");
        }
    }
}