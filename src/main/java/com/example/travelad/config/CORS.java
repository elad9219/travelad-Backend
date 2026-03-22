package com.example.travelad.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CORS {
    @Bean
    public CorsFilter corsFilter() {
        // Create new url configuration for browsers
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Create new cors configuration
        CorsConfiguration config = new CorsConfiguration();

        // Allow credentials (cookies, authorization headers, etc.)
        config.setAllowCredentials(true);

        // Allow any origin (Vercel, localhost, etc.)
        config.addAllowedOriginPattern("*");

        // Allow any header
        config.addAllowedHeader("*");

        // Allow specific methods
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");

        // Expose authorization header
        config.setExposedHeaders(List.of("authorization"));

        // Apply configuration to all routes
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}