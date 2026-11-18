package com.example.travelad.service;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class GooglePhotoUrlResolver {

    private static final Logger logger =
            LoggerFactory.getLogger(GooglePhotoUrlResolver.class);

    private final CloseableHttpClient client;

    public GooglePhotoUrlResolver(CloseableHttpClient client) {
        this.client = client;
    }

    public String resolve(URI photoRequest) {
        // Only the fixed Places photo endpoint may receive the server API key.
        if (!"https".equals(photoRequest.getScheme()) ||
                !"maps.googleapis.com".equals(photoRequest.getHost()) ||
                !"/maps/api/place/photo".equals(photoRequest.getPath()) ||
                photoRequest.getUserInfo() != null ||
                photoRequest.getPort() != -1 ||
                photoRequest.getFragment() != null) {
            return "";
        }

        HttpClientContext context = HttpClientContext.create();
        context.setRequestConfig(RequestConfig.custom()
                .setRedirectsEnabled(false)
                .setConnectionRequestTimeout(Timeout.ofSeconds(3))
                .setResponseTimeout(Timeout.ofSeconds(10))
                .build());

        // The shared client already disables automatic retries.
        // Inspect Location without following the redirect.
        try (ClassicHttpResponse response = client.executeOpen(
                null, new HttpGet(photoRequest), context)) {
            int status = response.getCode();

            if (status != 301 && status != 302 && status != 303 &&
                    status != 307 && status != 308) {
                return "";
            }

            Header location = response.getFirstHeader("Location");

            if (location == null || location.getValue().length() > 8192) {
                return "";
            }

            URI image = URI.create(location.getValue());
            String host = image.getHost();

            if (host == null) {
                return "";
            }

            host = host.toLowerCase(Locale.ROOT);

            if (!"https".equals(image.getScheme()) ||
                    !(host.equals("googleusercontent.com") ||
                            host.endsWith(".googleusercontent.com")) ||
                    image.getUserInfo() != null ||
                    (image.getPort() != -1 && image.getPort() != 443) ||
                    image.getFragment() != null) {
                return "";
            }

            String result = image.toASCIIString();
            String decoded = URLDecoder.decode(result, StandardCharsets.UTF_8);
            String key = queryValue(photoRequest, "key");

            if (key.isBlank() ||
                    result.contains(key) ||
                    decoded.contains(key)) {
                return "";
            }

            return result;
        } catch (Exception e) {
            // Exception messages can contain the API key or photo reference.
            logger.warn(
                    "City photo redirect unavailable ({})",
                    e.getClass().getSimpleName()
            );
            return "";
        }
    }

    private String queryValue(URI uri, String name) {
        String query = uri.getRawQuery();

        if (query == null) {
            return "";
        }

        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');

            if (separator > 0 && pair.substring(0, separator).equals(name)) {
                return URLDecoder.decode(
                        pair.substring(separator + 1),
                        StandardCharsets.UTF_8
                );
            }
        }

        return "";
    }
}