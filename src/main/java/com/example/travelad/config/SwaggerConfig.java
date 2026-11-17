package com.example.travelad.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI traveladApi() {
        return new OpenAPI().info(new Info()
                .title("Travelad Demo API")
                .version("1.0")
                .description("Portfolio demo with simulated flight and hotel offers."));
    }
}