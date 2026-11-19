package com.example.travelad.service;

import com.example.travelad.beans.Attraction;
import com.example.travelad.beans.AttractionCacheStatus;
import com.example.travelad.repositories.AttractionCacheStatusRepository;
import com.example.travelad.repositories.AttractionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AttractionsConcurrencyTest {

    private static final String LOCATION = """
            {"features":[{"properties":{"place_id":"test-city","lat":48.8,"lon":2.3,"country_code":"fr"}}]}
            """;

    private static final String EMPTY_PLACES = "{\"features\":[]}";

    private final AttractionRepository repository =
            mock(AttractionRepository.class);

    private final AttractionCacheStatusRepository statuses =
            mock(AttractionCacheStatusRepository.class);

    private final AttractionImageService images =
            mock(AttractionImageService.class);

    private final RestTemplate http = mock(RestTemplate.class);

    private final AttractionsService service =
            new AttractionsService(
                    repository, statuses, images, "test-key", http
            );

    private static Attraction cached(String city) {
        Attraction attraction = new Attraction();

        attraction.setCity(city);
        attraction.setName("Cached museum");
        attraction.setId(1L);
        attraction.setSelectionVersion(AttractionsService.VERSION);
        attraction.setDisplayOrder(0);

        return attraction;
    }

    private static void await(CountDownLatch latch)
            throws InterruptedException {

        assertTrue(
                latch.await(10, TimeUnit.SECONDS),
                "Timed out waiting for concurrent request"
        );
    }

    @Test
    void twoRefreshesCanRunWhileCachedRequestsBypassCapacity()
            throws Exception {

        Attraction fresh = cached("rome");
        Attraction stale = cached("madrid");

        AttractionCacheStatus freshStatus =
                new AttractionCacheStatus("rome", true);

        freshStatus.setVersion(AttractionsService.VERSION);
        freshStatus.setRefreshedAt(System.currentTimeMillis());

        when(statuses.findById("rome"))
                .thenReturn(Optional.of(freshStatus));

        when(repository.findByCityIgnoreCase("rome"))
                .thenReturn(List.of(fresh));

        when(repository.findByCityIgnoreCase("madrid"))
                .thenReturn(List.of(stale));

        CountDownLatch bothEntered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        AtomicInteger discoveryCalls = new AtomicInteger();

        when(http.getForObject(any(URI.class), eq(String.class)))
                .thenAnswer(call -> {
                    URI uri = call.getArgument(0);

                    if (uri.getPath().endsWith("/geocode/search")) {
                        discoveryCalls.incrementAndGet();
                        bothEntered.countDown();

                        await(release);

                        return LOCATION;
                    }

                    return EMPTY_PLACES;
                });

        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            Future<List<Attraction>> paris = executor.submit(
                    () -> service.searchPlacesByCity("Paris")
            );

            Future<List<Attraction>> london = executor.submit(
                    () -> service.searchPlacesByCity("London")
            );

            await(bothEntered);

            assertEquals(
                    List.of(fresh),
                    executor.submit(() -> service.searchPlacesByCity("Rome"))
                            .get(5, TimeUnit.SECONDS)
            );

            assertEquals(
                    List.of(stale),
                    executor.submit(() -> service.searchPlacesByCity("Madrid"))
                            .get(5, TimeUnit.SECONDS)
            );

            Future<List<Attraction>> unavailable = executor.submit(
                    () -> service.searchPlacesByCity("Berlin")
            );

            ExecutionException failure = assertThrows(
                    ExecutionException.class,
                    () -> unavailable.get(5, TimeUnit.SECONDS)
            );

            assertInstanceOf(IllegalStateException.class, failure.getCause());
            assertEquals(2, discoveryCalls.get());

            verify(statuses, never())
                    .save(any(AttractionCacheStatus.class));

            release.countDown();

            assertTrue(paris.get(5, TimeUnit.SECONDS).isEmpty());
            assertTrue(london.get(5, TimeUnit.SECONDS).isEmpty());

            assertTrue(service.searchPlacesByCity("Berlin").isEmpty());
            assertEquals(3, discoveryCalls.get());

        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void identicalNormalizedRequestsShareOneRefresh() throws Exception {

        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        AtomicInteger discoveryCalls = new AtomicInteger();

        when(http.getForObject(any(URI.class), eq(String.class)))
                .thenAnswer(call -> {
                    URI uri = call.getArgument(0);

                    if (uri.getPath().endsWith("/geocode/search")) {
                        discoveryCalls.incrementAndGet();
                        entered.countDown();

                        await(release);

                        return LOCATION;
                    }

                    return EMPTY_PLACES;
                });

        FutureTask<List<Attraction>> first = new FutureTask<>(
                () -> service.searchPlacesByCity("Paris")
        );

        FutureTask<List<Attraction>> second = new FutureTask<>(
                () -> service.searchPlacesByCity("  PARIS  ")
        );

        Thread owner = Thread.ofPlatform().daemon().unstarted(first);
        Thread waiter = Thread.ofPlatform().daemon().unstarted(second);

        owner.start();

        try {
            await(entered);

            waiter.start();

            long deadline = System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(5);

            while (waiter.getState() != Thread.State.TIMED_WAITING
                    && System.nanoTime() < deadline) {
                Thread.sleep(1);
            }

            assertEquals(Thread.State.TIMED_WAITING, waiter.getState());
            assertEquals(1, discoveryCalls.get());

            release.countDown();

            assertSame(
                    first.get(5, TimeUnit.SECONDS),
                    second.get(5, TimeUnit.SECONDS)
            );

            verify(repository, times(1)).saveAll(anyList());

        } finally {
            release.countDown();
            owner.interrupt();
            waiter.interrupt();
        }
    }

    @Test
    void failedRefreshDoesNotLeakPermitOrInFlightEntry() {

        CityRefreshCoordinator<String> coordinator =
                new CityRefreshCoordinator<>(1);

        assertThrows(
                IllegalStateException.class,
                () -> coordinator.load(
                        "paris",
                        () -> coordinator.refresh(
                                () -> {
                                    throw new IllegalStateException(
                                            "Provider unavailable"
                                    );
                                },
                                () -> "busy"
                        )
                )
        );

        assertEquals(
                "recovered",
                coordinator.load(
                        "paris",
                        () -> coordinator.refresh(
                                () -> "recovered",
                                () -> "busy"
                        )
                )
        );
    }
}