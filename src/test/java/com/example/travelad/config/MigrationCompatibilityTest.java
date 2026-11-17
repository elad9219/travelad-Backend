package com.example.travelad.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
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

    @Test
    void legacyReadinessStillReturnsSwaggerMetadata() throws Exception {
        mvc.perform(get("/v2/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.swagger").value("2.0"))
                .andExpect(jsonPath("$.paths").isMap());
    }

    @Test
    void browserCanReadTheReadinessResponse() throws Exception {
        mvc.perform(get("/v2/api-docs").header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void demoFlightJsonKeepsItsExistingShape() throws Exception {
        mvc.perform(get("/flights").param("city", "Paris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].outboundSegments[0].origin").value("TLV"))
                .andExpect(jsonPath("$[0].returnSegments[0].origin").value("PAR"))
                .andExpect(jsonPath("$[0].price", greaterThan(0.0)));
    }
}