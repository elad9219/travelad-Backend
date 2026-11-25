package com.example.travelad.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:migration-compatibility;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "google.places.api.key=test-not-a-real-key",
        "geoapify.api.key=test-not-a-real-key",
        "weatherapi.api.key=test-not-a-real-key",
        "weatherapi.base.url=https://api.weatherapi.com/v1"
})
@AutoConfigureMockMvc
class MigrationCompatibilityTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper httpJson;

    @Test
    void defaultHttpJsonMapperPreservesNullExclusion() throws Exception {
        var hotel = new com.example.travelad.dto.HotelDto(
                "Demo Hotel", "MOCK_1", "PAR", "FR", null, null);

        var result = httpJson.readTree(httpJson.writeValueAsString(hotel));

        assertFalse(result.has("latitude"));
        assertFalse(result.has("longitude"));
        assertEquals("Demo Hotel", result.path("name").asText());
    }

    @Test
    void readinessReturnsOpenApiMetadata() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.")))
                .andExpect(jsonPath("$.paths").isMap());
    }

    @Test
    void browserCanReadTheReadinessResponse() throws Exception {
        mvc.perform(get("/v3/api-docs").header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void removedLegacyDocumentationReturnsNotFound() throws Exception {
        mvc.perform(get("/v2/api-docs"))
                .andExpect(status().isNotFound());
    }

    @Test
    void demoFlightJsonKeepsItsExistingShape() throws Exception {
        mvc.perform(get("/flights").param("city", "Paris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("MOCK"))
                .andExpect(jsonPath("$[0].segments").isArray())
                .andExpect(jsonPath("$[0].outboundSegments[0].origin").value("TLV"))
                .andExpect(jsonPath("$[0].outboundSegments[-1].destination")
                        .value(isOneOf("CDG", "ORY")))
                .andExpect(jsonPath("$[0].returnSegments[0].origin")
                        .value(isOneOf("CDG", "ORY")))
                .andExpect(jsonPath("$[0].returnSegments[-1].destination").value("TLV"))
                .andExpect(jsonPath("$[0].price", greaterThan(0.0)));
    }
}