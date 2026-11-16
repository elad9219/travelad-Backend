package com.example.travelad.controller;

import com.example.travelad.exceptions.ExternalApiException;
import com.example.travelad.exceptions.InvalidInputException;
import com.example.travelad.service.CityCacheService;
import com.example.travelad.service.GooglePlacesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        GooglePlacesController.class,
        CityCacheController.class
})
@Import(ApiErrorHandlingTest.FailureController.class)
class ApiErrorHandlingTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GooglePlacesService places;

    @MockBean
    private CityCacheService history;

    @RestController
    static class FailureController {

        @GetMapping("/test/unexpected-error")
        public void fail() {
            throw new IllegalStateException("private-secret");
        }

        @GetMapping("/test/invalid-input")
        public void invalid() {
            throw new InvalidInputException("private-secret");
        }

        @GetMapping("/test/provider-error")
        public void provider() {
            throw new ExternalApiException("private-secret");
        }
    }

    @Test
    void emptyCityRemainsBadRequestWithApplicationAdviceLoaded() throws Exception {
        mvc.perform(get("/api/places/search").param("city", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void missingParameterRemainsBadRequest() throws Exception {
        mvc.perform(get("/api/places/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void unsupportedMethodRemainsMethodNotAllowed() throws Exception {
        mvc.perform(put("/api/places/search").param("city", "Paris"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")));
    }

    @Test
    void unexpectedErrorDoesNotExposeItsMessage() throws Exception {
        mvc.perform(get("/test/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(content().string(not(containsString("private-secret"))));
    }

    @Test
    void customInputErrorRemainsBadRequestWithoutExposingItsMessage() throws Exception {
        mvc.perform(get("/test/invalid-input"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("private-secret"))));
    }

    @Test
    void providerErrorIsBadGatewayWithoutExposingItsMessage() throws Exception {
        mvc.perform(get("/test/provider-error"))
                .andExpect(status().isBadGateway())
                .andExpect(content().string(not(containsString("private-secret"))));
    }

    @Test
    void bothHistoryRoutesUseTheSameApplicationErrorHandling() throws Exception {
        for (String base : new String[]{"/cache", "/api/places/cache"}) {
            mvc.perform(get(base + "/cities").param("userId", "bad:id"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }
    }
}