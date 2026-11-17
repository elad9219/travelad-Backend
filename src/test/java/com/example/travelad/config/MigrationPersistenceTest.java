package com.example.travelad.config;

import com.example.travelad.beans.Attraction;
import com.example.travelad.beans.AttractionCacheStatus;
import com.example.travelad.beans.GooglePlaces;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MigrationPersistenceTest {

    @Autowired
    private TestEntityManager entities;

    @Test
    void generatedIdsAndExistingAttractionFieldsRoundTrip() {
        Attraction attraction = new Attraction();
        attraction.setName("Museum 東京");
        attraction.setCity("tokyo");
        attraction.setDescription("Long description ".repeat(100));
        attraction.setLatitude(35.7);
        attraction.setLongitude(139.7);
        attraction.setSelectionVersion(3);
        attraction.setDisplayOrder(2);

        entities.persistAndFlush(attraction);
        Long id = attraction.getId();
        entities.clear();

        Attraction stored = entities.find(Attraction.class, id);

        assertNotNull(id);
        assertEquals("Museum 東京", stored.getName());
        assertEquals(attraction.getDescription(), stored.getDescription());
        assertEquals(3, stored.getSelectionVersion());
        assertEquals(2, stored.getDisplayOrder());
        assertEquals(35.7, stored.getLatitude());
    }

    @Test
    void placeAndCacheMappingsKeepTheirIdentifiersAndStatus() {
        GooglePlaces place = new GooglePlaces();
        place.setPlaceId("migration-test-place");
        place.setCity("tokyo");
        place.setName("Tokyo");
        place.setComplete(true);
        entities.persistAndFlush(place);

        AttractionCacheStatus cache = new AttractionCacheStatus("tokyo", true);
        cache.setVersion(3);
        entities.persistAndFlush(cache);
        entities.clear();

        assertTrue(entities.find(GooglePlaces.class, place.getId()).isComplete());

        AttractionCacheStatus stored =
                entities.find(AttractionCacheStatus.class, "tokyo");

        assertEquals(3, stored.getVersion());
        assertTrue(stored.isComplete());
    }
}