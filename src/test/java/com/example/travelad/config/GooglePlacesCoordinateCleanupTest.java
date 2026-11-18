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