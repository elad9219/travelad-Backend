package com.example.travelad.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public CloseableHttpClient outboundHttpClient() {
        RequestConfig timeouts = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(3))
                .setResponseTimeout(Timeout.ofSeconds(10))
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(timeouts)
                .disableAutomaticRetries()
                .build();
    }

    @Bean
    public RestTemplate restTemplate(CloseableHttpClient outboundHttpClient) {
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(outboundHttpClient);

        factory.setConnectTimeout(3000);
        factory.setConnectionRequestTimeout(3000);

        return new RestTemplate(factory);
    }
}