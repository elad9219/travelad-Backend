package com.example.travelad.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public CloseableHttpClient outboundHttpClient() {
        return HttpClients.custom()
                .disableAutomaticRetries()
                .build();
    }

    @Bean
    public RestTemplate restTemplate(CloseableHttpClient outboundHttpClient) {
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(outboundHttpClient);

        factory.setConnectTimeout(3000);
        factory.setConnectionRequestTimeout(3000);
        factory.setReadTimeout(10000);

        return new RestTemplate(factory);
    }
}