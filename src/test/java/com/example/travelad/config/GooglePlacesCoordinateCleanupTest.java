package com.example.travelad.config;

import com.example.travelad.beans.GooglePlaces;
import com.example.travelad.repositories.GooglePlacesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class GooglePlacesCoordinateCleanupTest {

    @Autowired
    private TestEntityManager entities;

    @Autowired
    private GooglePlacesRepository repository;

    @Test
    void clearsOnlyExpiredOrUndatedCoordinatesAndPreservesPlaceIds() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 10, 20, 12, 0);
        Long expiredId = store("expired", cutoff);
        Long undatedId = store("undated", null);
        Long freshId = store("fresh", cutoff.plusSeconds(1));

        assertEquals(2, repository.clearExpiredCoordinates(cutoff));
        assertEquals(0, repository.clearExpiredCoordinates(cutoff));
        entities.clear();

        GooglePlaces expired = entities.find(GooglePlaces.class, expiredId);
        assertEquals("expired", expired.getPlaceId());
        assertNull(expired.getLatitude());
        assertNull(expired.getLongitude());

        GooglePlaces undated = entities.find(GooglePlaces.class, undatedId);
        assertEquals("undated", undated.getPlaceId());
        assertNull(undated.getLatitude());
        assertNull(undated.getLongitude());

        GooglePlaces fresh = entities.find(GooglePlaces.class, freshId);
        assertEquals("fresh", fresh.getPlaceId());
        assertEquals(48.85, fresh.getLatitude());
        assertEquals(2.35, fresh.getLongitude());
    }

    @Test
    void clearsLegacyDisplayDetailsWithoutChangingUsableCoordinates() {
        LocalDateTime fetchedAt = LocalDateTime.of(2026, 11, 12, 12, 0);
        Long id = store("legacy", fetchedAt);
        GooglePlaces legacy = entities.find(GooglePlaces.class, id);
        legacy.setAddress("Paris, France");
        legacy.setCountry("France");
        legacy.setIcon("https://example.test/legacy-photo");
        entities.flush();

        assertEquals(1, repository.clearStoredDisplayDetails());
        assertEquals(0, repository.clearStoredDisplayDetails());
        entities.clear();

        GooglePlaces cleaned = entities.find(GooglePlaces.class, id);
        assertEquals("legacy", cleaned.getPlaceId());
        assertEquals("paris", cleaned.getCity());
        assertEquals(fetchedAt, cleaned.getCreatedAt());
        assertEquals(48.85, cleaned.getLatitude());
        assertEquals(2.35, cleaned.getLongitude());
        assertNull(cleaned.getName());
        assertNull(cleaned.getAddress());
        assertNull(cleaned.getCountry());
        assertNull(cleaned.getIcon());
    }

    private Long store(String placeId, LocalDateTime fetchedAt) {
        GooglePlaces place = new GooglePlaces();
        place.setPlaceId(placeId);
        place.setCity("paris");
        place.setName("Paris");
        place.setLatitude(48.85);
        place.setLongitude(2.35);
        place.setCreatedAt(fetchedAt);
        return entities.persistAndFlush(place).getId();
    }
}