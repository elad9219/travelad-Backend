package com.example.travelad.service;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.example.travelad.beans.Attraction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.*;

class AttractionImageServiceTest {
    private final ObjectMapper json = new ObjectMapper();
    private final RestTemplate http = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(http).build();
    private final AttractionImageService service = new AttractionImageService(http);

    private Attraction place() {
        Attraction a = new Attraction();
        a.setName("浅草寺");
        a.setLatitude(35.7148);
        a.setLongitude(139.7967);
        return a;
    }

    private JsonNode props() throws Exception {
        return json.readTree(
                "{\"name\":\"浅草寺\",\"categories\":[\"tourism.sights.place_of_worship.temple\"],\"datasource\":{\"raw\":{\"wikidata\":\"Q615183\"}}}"
        );
    }

    private String entity(double latitude) {
        return "{\"entities\":{\"Q615183\":{\"labels\":{\"en\":{\"value\":\"Senso-ji\"}},"
                + "\"sitelinks\":{\"enwiki\":{},\"jawiki\":{}},\"claims\":{"
                + "\"P625\":[{\"rank\":\"normal\",\"mainsnak\":{\"datavalue\":{\"value\":{"
                + "\"latitude\":" + latitude
                + ",\"longitude\":139.7967,\"globe\":\"http://www.wikidata.org/entity/Q2\"}}}}],"
                + "\"P18\":[{\"rank\":\"normal\",\"mainsnak\":{\"datavalue\":{\"value\":\"Temple.jpg\"}}}]}}}}";
    }

    private ObjectNode info(String license, String host) throws Exception {
        return (ObjectNode) json.readTree(
                "{\"thumburl\":\"https://" + host + "/wikipedia/commons/a/a1/Temple.jpg\","
                        + "\"descriptionurl\":\"https://commons.wikimedia.org/wiki/File:Temple.jpg\","
                        + "\"mime\":\"image/jpeg\",\"width\":1200,\"height\":800,"
                        + "\"extmetadata\":{\"LicenseShortName\":{\"value\":\"" + license + "\"},"
                        + "\"Artist\":{\"value\":\"<a>Photographer</a>\"}}}"
        );
    }

    private String fileResponse(String license, String host) throws Exception {
        return "{\"query\":{\"pages\":{\"1\":{\"title\":\"File:Temple.jpg\",\"imageinfo\":["
                + info(license, host)
                + "]}}}}";
    }

    @Test
    void usesEntityIdentityWithoutRequiringTranslatedNameMatch() throws Exception {
        server.expect(requestTo(containsString("www.wikidata.org")))
                .andExpect(queryParam("ids", "Q615183"))
                .andRespond(withSuccess(entity(35.7148), MediaType.APPLICATION_JSON));

        server.expect(requestTo(containsString("commons.wikimedia.org")))
                .andExpect(queryParam("titles", "File:Temple.jpg"))
                .andRespond(withSuccess(
                        fileResponse("CC BY-SA 4.0", "thumb.wikimedia.org"),
                        MediaType.APPLICATION_JSON
                ));

        Attraction a = place();
        AttractionImageService.Result result = service.enrich(
                List.of(a), Map.of(a, props()), "jp"
        );

        assertTrue(result.complete);
        assertEquals("Senso-ji", a.getName());
        assertEquals(2, result.sitelinks.get(a));
        assertNotNull(a.getImageUrl());
        server.verify();
    }

    @Test
    void usesLinkedArticleImageWhenEntityHasNoPrimaryImage() throws Exception {
        ObjectNode response = (ObjectNode) json.readTree(entity(35.7148));

        ((ObjectNode) response.path("entities").path("Q615183").path("claims"))
                .remove("P18");
        ((ObjectNode) response.path("entities").path("Q615183").path("sitelinks").path("enwiki"))
                .put("title", "Canonical temple");

        server.expect(requestTo(containsString("www.wikidata.org")))
                .andRespond(withSuccess(response.toString(), MediaType.APPLICATION_JSON));

        server.expect(requestTo(containsString("en.wikipedia.org")))
                .andRespond(withSuccess(
                        "{\"query\":{\"pages\":{\"1\":{\"title\":\"Canonical temple\","
                                + "\"pageprops\":{\"wikibase_item\":\"Q615183\"},"
                                + "\"pageimage\":\"Temple.jpg\"}}}}",
                        MediaType.APPLICATION_JSON
                ));

        server.expect(requestTo(containsString("commons.wikimedia.org")))
                .andRespond(withSuccess(
                        fileResponse("CC BY-SA 4.0", "thumb.wikimedia.org"),
                        MediaType.APPLICATION_JSON
                ));

        Attraction a = place();

        assertTrue(service.enrich(List.of(a), Map.of(a, props()), "jp").complete);
        assertNotNull(a.getImageUrl());
        server.verify();
    }

    @Test
    void refusesAnEntityInAnotherLocation() throws Exception {
        server.expect(requestTo(containsString("www.wikidata.org")))
                .andRespond(withSuccess(entity(36.0), MediaType.APPLICATION_JSON));

        Attraction a = place();

        assertTrue(service.enrich(List.of(a), Map.of(a, props()), "jp").complete);
        assertNull(a.getImageUrl());
        assertEquals("浅草寺", a.getName());
        server.verify();
    }

    @Test
    void titleFallbackFollowsRedirectsAndRequiresCoordinates() throws Exception {
        JsonNode properties = json.readTree(
                "{\"name\":\"浅草寺\",\"categories\":[\"entertainment.museum\"]}"
        );

        server.expect(requestTo(containsString("en.wikipedia.org")))
                .andRespond(withSuccess(
                        "{\"query\":{\"redirects\":[{\"from\":\"浅草寺\",\"to\":\"Temple\"}],"
                                + "\"pages\":{\"1\":{\"title\":\"Temple\",\"pageimage\":\"Temple.jpg\","
                                + "\"coordinates\":[{\"lat\":35.7148,\"lon\":139.7967}]}}}}",
                        MediaType.APPLICATION_JSON
                ));

        server.expect(requestTo(containsString("commons.wikimedia.org")))
                .andRespond(withSuccess(
                        fileResponse("CC0", "upload.wikimedia.org"),
                        MediaType.APPLICATION_JSON
                ));

        Attraction a = place();

        assertTrue(service.enrich(List.of(a), Map.of(a, properties), "us").complete);
        assertNotNull(a.getImageUrl());
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({
            "CC BY-SA 4.0, upload.wikimedia.org, true",
            "CC BY-SA 4.0, thumb.wikimedia.org, true",
            "CC0, thumb.wikimedia.org, true",
            "Public domain, thumb.wikimedia.org, true",
            "CC BY-NC 4.0, thumb.wikimedia.org, false",
            "All rights reserved, thumb.wikimedia.org, false",
            "CC BY-SA 4.0, thumb.wikimedia.org.evil.test, false"
    })
    void validatesLicenseAndExactImageHost(
            String license, String host, boolean accepted
    ) throws Exception {
        Attraction a = place();

        assertEquals(accepted, AttractionImageService.applyImage(a, info(license, host)));

        if (accepted) {
            assertEquals("Photographer", a.getImageAttribution());
        } else {
            assertNull(a.getImageUrl());
        }
    }

    @Test
    void rejectsDiagramsTinyImagesAndMissingRequiredCredit() throws Exception {
        ObjectNode diagram = info("CC BY-SA 4.0", "thumb.wikimedia.org");
        diagram.put("mime", "image/svg+xml");
        assertFalse(AttractionImageService.applyImage(place(), diagram));

        ObjectNode tiny = info("CC BY-SA 4.0", "thumb.wikimedia.org");
        tiny.put("width", 100);
        assertFalse(AttractionImageService.applyImage(place(), tiny));

        ObjectNode noCredit = info("CC BY-SA 4.0", "thumb.wikimedia.org");
        ((ObjectNode) noCredit.path("extmetadata")).remove("Artist");
        assertFalse(AttractionImageService.applyImage(place(), noCredit));
    }

    @Test
    void rejectsMissingCoordinatesRatherThanAssumingZero() throws Exception {
        assertFalse(AttractionImageService.near(place(), json.readTree("{}"), 1500));
        assertFalse(AttractionImageService.near(
                place(), json.readTree("{\"lat\":95,\"lon\":139}"), 1500
        ));
        assertEquals(
                AttractionImageService.normalize("Château"),
                AttractionImageService.normalize("Chateau")
        );
    }

    @Test
    void providerFailureDoesNotMasqueradeAsCompletedLookup() throws Exception {
        server.expect(requestTo(containsString("www.wikidata.org")))
                .andRespond(withServerError());

        Attraction a = place();

        assertFalse(service.enrich(List.of(a), Map.of(a, props()), "jp").complete);
        assertNull(a.getImageUrl());
        server.verify();
    }

    @Test
    void requestsCoordinatesForEntireBoundedTitleBatch() throws Exception {
        List<Attraction> places = new ArrayList<>();
        Map<Attraction, JsonNode> properties = new LinkedHashMap<>();
        List<String> titles = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            Attraction a = place();
            a.setName("Place " + i);
            places.add(a);
            properties.put(a, json.readTree("{}"));

            if (i < 50) {
                titles.add(a.getName());
            }
        }

        server.expect(requestTo(containsString("en.wikipedia.org")))
                .andExpect(queryParam("colimit", "50"))
                .andExpect(request -> assertTrue(
                        request.getURI().getQuery().contains(
                                "titles=" + String.join("|", titles) + "&"
                        )
                ))
                .andRespond(withSuccess(
                        "{\"query\":{\"pages\":{}}}",
                        MediaType.APPLICATION_JSON
                ));

        assertTrue(service.enrich(places, properties, "us").complete);
        server.verify();
    }

    @Test
    void restrictsCreditUrlsAndCredentials() {
        assertFalse(AttractionImageService.safeUrl(
                "https://user@commons.wikimedia.org/a", "commons.wikimedia.org"
        ));
        assertFalse(AttractionImageService.safeUrl(
                "http://commons.wikimedia.org/a", "commons.wikimedia.org"
        ));
        assertFalse(AttractionImageService.safeUrl(
                "https://commons.wikimedia.org.evil.test/a", "commons.wikimedia.org"
        ));
    }
}