package com.example.travelad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:application-context;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.open-in-view=false",
		"spring.data.redis.url=redis://127.0.0.1:1/0",
		"spring.data.redis.repositories.enabled=false",
		"google.places.api.key=test-not-a-real-key",
		"geoapify.api.key=test-not-a-real-key",
		"weatherapi.api.key=test-not-a-real-key",
		"weatherapi.base.url=http://127.0.0.1:1"
})
class TraveladApplicationTests {

	@Autowired
	private DataSource database;

	@Autowired
	private LettuceConnectionFactory redis;

	@Test
	void contextLoadsWithIsolatedTestConnections() throws Exception {
		try (var connection = database.getConnection()) {
			assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
		}

		assertNotNull(redis.getStandaloneConfiguration());
		assertEquals("127.0.0.1", redis.getStandaloneConfiguration().getHostName());
		assertEquals(1, redis.getStandaloneConfiguration().getPort());
	}
}