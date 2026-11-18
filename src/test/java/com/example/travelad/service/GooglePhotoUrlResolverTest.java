package com.example.travelad.service;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GooglePhotoUrlResolverTest {

    private static final URI REQUEST = URI.create(
            "https://maps.googleapis.com/maps/api/place/photo?maxwidth=1200" +
                    "&photo_reference=fresh-reference&key=server-secret"
    );

    private final CloseableHttpClient client = mock(CloseableHttpClient.class);
    private final GooglePhotoUrlResolver resolver = new GooglePhotoUrlResolver(client);

    private void reply(int status, String location) throws IOException {
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(status);

        if (location != null) {
            response.addHeader("Location", location);
        }

        when(client.executeOpen(
                isNull(),
                any(HttpGet.class),
                any(HttpClientContext.class)
        )).thenReturn(response);
    }

    @Test
    void resolvesTheCdnUrlWithoutFollowingTheRedirectOrExposingTheKey()
            throws Exception {
        reply(302, "https://lh3.googleusercontent.com/places/public-image=w1200");

        assertEquals(
                "https://lh3.googleusercontent.com/places/public-image=w1200",
                resolver.resolve(REQUEST)
        );

        ArgumentCaptor<HttpClientContext> context =
                ArgumentCaptor.forClass(HttpClientContext.class);

        verify(client, times(1)).executeOpen(
                isNull(),
                any(HttpGet.class),
                context.capture()
        );

        assertFalse(context.getValue().getRequestConfig().isRedirectsEnabled());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://lh3.googleusercontent.com/image",
            "https://lh3.googleusercontent.com.evil.example/image",
            "https://evilgoogleusercontent.com/image",
            "https://127.0.0.1/image",
            "https://user:password@lh3.googleusercontent.com/image",
            "https://lh3.googleusercontent.com:8443/image",
            "https://lh3.googleusercontent.com/image#fragment",
            "https://maps.googleapis.com/maps/api/place/photo?key=server-secret",
            "https://lh3.googleusercontent.com/image?key=server-secret",
            "https://lh3.googleusercontent.com/image?key=%73erver-secret",
            "/relative-image",
            "not a URI"
    })
    void rejectsUnexpectedOrSecretBearingRedirects(String location)
            throws Exception {
        reply(302, location);

        assertEquals("", resolver.resolve(REQUEST));

        verify(client, times(1)).executeOpen(
                isNull(),
                any(HttpGet.class),
                any(HttpClientContext.class)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 400, 403, 429, 500})
    void neverReturnsTheAuthenticatedRequestAsAFallback(int status)
            throws Exception {
        reply(status, "https://lh3.googleusercontent.com/image");

        assertEquals("", resolver.resolve(REQUEST));
    }

    @Test
    void missingRedirectAndTransportFailuresAreOptionalPhotoFailures()
            throws Exception {
        reply(302, null);
        assertEquals("", resolver.resolve(REQUEST));

        when(client.executeOpen(
                isNull(),
                any(HttpGet.class),
                any(HttpClientContext.class)
        )).thenThrow(new IOException("Request containing server-secret failed"));

        assertEquals("", resolver.resolve(REQUEST));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://maps.googleapis.com/maps/api/place/photo?key=server-secret",
            "https://evil.example/maps/api/place/photo?key=server-secret",
            "https://maps.googleapis.com/other?key=server-secret",
            "https://user@maps.googleapis.com/maps/api/place/photo?key=server-secret"
    })
    void rejectsArbitraryRequestTargetsWithoutSendingAnything(String value) {
        assertEquals("", resolver.resolve(URI.create(value)));

        verifyNoInteractions(client);
    }
}