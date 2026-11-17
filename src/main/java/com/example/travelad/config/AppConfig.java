package com.example.travelad.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
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

        var connections = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(3))
                        .setSocketTimeout(Timeout.ofSeconds(10))
                        .build())
                .build();

        return HttpClients.custom()
                .setConnectionManager(connections)
                .setDefaultRequestConfig(timeouts)
                .disableAutomaticRetries()
                .build();
    }

    @Bean
    public RestTemplate restTemplate(CloseableHttpClient outboundHttpClient) {
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(outboundHttpClient);

        factory.setConnectionRequestTimeout(3000);

        return new RestTemplate(factory);
    }
}