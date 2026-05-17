package com.jjs.patentapi.client;

import com.jjs.patentapi.dto.PatentDetailResponse;
import com.jjs.patentapi.dto.PatentSearchRequest;
import com.jjs.patentapi.dto.PatentSearchResponse;
import com.jjs.patentapi.dto.TranslationRequest;
import com.jjs.patentapi.dto.TranslationResponse;
import com.jjs.patentapi.service.PatentSearchService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalClientTests {

    @Test
    void serpApiSearchMapsOrganicResults() throws Exception {
        try (TestHttpServer server = TestHttpServer.start(exchange -> {
            Map<String, String> query = queryParams(exchange);
            assertThat(query).containsEntry("engine", "google_patents");
            assertThat(query).containsEntry("q", "battery");
            assertThat(query).containsEntry("country", "JP");
            assertThat(query).containsEntry("page", "2");
            assertThat(query).containsEntry("num", "10");
            assertThat(query).containsEntry("after", "filing:20200101");
            assertThat(query).containsEntry("before", "filing:20251231");
            assertThat(query).containsEntry("api_key", "serp-key");

            respondJson(exchange, """
                    {
                      "search_information": { "total_results": 42 },
                      "organic_results": [
                        {
                          "patent_id": "patent/US8110241B2/en",
                          "title": "Foaming soluble coffee powder containing pressurized gas",
                          "assignee": "Kraft Foods Global Brands Llc",
                          "filing_date": "2010-04-27",
                          "publication_number": "JP8110241B2"
                        },
                        {
                          "patent_id": "patent/RU2766609C2/en",
                          "title": "Second result",
                          "assignee": "Nestle",
                          "filing_date": "2017-12-22",
                          "publication_number": "RU2766609C2"
                        }
                      ]
                    }
                    """);
        })) {
            SerpApiClient client = new SerpApiClient(RestClient.builder(), "serp-key", server.baseUrl());
            PatentSearchRequest request = new PatentSearchRequest();
            request.setQuery("battery");
            request.setFrom("2020-01-01");
            request.setTo("2025-12-31");
            request.setPage(2);
            request.setSize(1);

            PatentSearchResponse response = client.search(request);

            assertThat(response.getTotalCount()).isEqualTo(42);
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getId()).isEqualTo("JP8110241B2");
            assertThat(response.getItems().get(0).getApplicant()).isEqualTo("Kraft Foods Global Brands Llc");
        }
    }

    @Test
    void serpApiDetailNormalizesPublicationNumberAndMapsResponse() throws Exception {
        try (TestHttpServer server = TestHttpServer.start(exchange -> {
            Map<String, String> query = queryParams(exchange);
            assertThat(query).containsEntry("engine", "google_patents_details");
            assertThat(query).containsEntry("patent_id", "patent/US11734097B1");

            respondJson(exchange, """
                    {
                      "search_metadata": {
                        "google_patents_details_url": "https://patents.google.com/patent/US11734097B1/en"
                      },
                      "title": "Machine learning-based hardware component monitoring",
                      "abstract": "A monitoring system detects anomalies.",
                      "assignees": ["Pure Storage Inc"],
                      "filing_date": "2021-01-27",
                      "publication_number": "US11734097B1"
                    }
                    """);
        })) {
            SerpApiClient client = new SerpApiClient(RestClient.builder(), "serp-key", server.baseUrl());

            PatentDetailResponse response = client.findById("US11734097B1");

            assertThat(response.getId()).isEqualTo("US11734097B1");
            assertThat(response.getApplicant()).isEqualTo("Pure Storage Inc");
            assertThat(response.getOriginalUrl()).isEqualTo("https://patents.google.com/patent/US11734097B1/en");
        }
    }

    @Test
    void deepLTranslatePostsJsonBodyAndMapsFirstTranslation() throws Exception {
        try (TestHttpServer server = TestHttpServer.start(exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("POST");
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("DeepL-Auth-Key deepl-key");
            assertThat(exchange.getRequestHeaders().getFirst("Content-Type")).contains("application/json");
            assertThat(exchange.getRequestHeaders().getFirst("Accept")).contains("application/json");
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(body).contains("\"text\":[\"Hello\"]");
            assertThat(body).contains("\"source_lang\":\"EN\"");
            assertThat(body).contains("\"target_lang\":\"KO\"");

            respondJson(exchange, """
                    {
                      "translations": [
                        {
                          "detected_source_language": "EN",
                          "text": "안녕하세요"
                        }
                      ]
                    }
                    """);
        })) {
            DeepLClient client = new DeepLClient(RestClient.builder(), "deepl-key", server.baseUrl());
            TranslationRequest request = new TranslationRequest();
            request.setSourceText("Hello");
            request.setSourceLang("EN");
            request.setTargetLang("KO");

            TranslationResponse response = client.translate(request);

            assertThat(response.getTranslatedText()).isEqualTo("안녕하세요");
        }
    }

    @Test
    void deepLTranslateNormalizesCommonLanguageAliases() throws Exception {
        try (TestHttpServer server = TestHttpServer.start(exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(body).contains("\"source_lang\":\"JA\"");
            assertThat(body).contains("\"target_lang\":\"KO\"");

            respondJson(exchange, """
                    {
                      "translations": [
                        { "text": "특허" }
                      ]
                    }
                    """);
        })) {
            DeepLClient client = new DeepLClient(RestClient.builder(), "deepl-key", server.baseUrl());
            TranslationRequest request = new TranslationRequest();
            request.setSourceText("特許");
            request.setSourceLang("JP");
            request.setTargetLang("KR");

            TranslationResponse response = client.translate(request);

            assertThat(response.getTranslatedText()).isEqualTo("특허");
        }
    }

    @Test
    void searchServiceTranslatesKoreanQueryToJapaneseBeforeSerpApiSearch() throws Exception {
        try (
                TestHttpServer deepLServer = TestHttpServer.start(exchange -> {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    assertThat(body).contains("\"text\":[\"배터리\"]");
                    assertThat(body).contains("\"source_lang\":\"KO\"");
                    assertThat(body).contains("\"target_lang\":\"JA\"");

                    respondJson(exchange, """
                            {
                              "translations": [
                                { "text": "バッテリー" }
                              ]
                            }
                            """);
                });
                TestHttpServer serpServer = TestHttpServer.start(exchange -> {
                    Map<String, String> query = queryParams(exchange);
                    assertThat(query).containsEntry("engine", "google_patents");
                    assertThat(query).containsEntry("q", "バッテリー");
                    assertThat(query).containsEntry("country", "JP");

                    respondJson(exchange, """
                            {
                              "search_information": { "total_results": 1 },
                              "organic_results": [
                                {
                                  "title": "Battery patent",
                                  "assignee": "Example Corp",
                                  "filing_date": "2024-01-10",
                                  "publication_number": "JP2024000001"
                                }
                              ]
                            }
                            """);
                })
        ) {
            PatentSearchService service = new PatentSearchService(
                    new SerpApiClient(RestClient.builder(), "serp-key", serpServer.baseUrl()),
                    new DeepLClient(RestClient.builder(), "deepl-key", deepLServer.baseUrl())
            );
            PatentSearchRequest request = new PatentSearchRequest();
            request.setQuery("배터리");

            PatentSearchResponse response = service.search(request);

            assertThat(response.getQuery()).isEqualTo("배터리");
            assertThat(response.getItems()).hasSize(1);
        }
    }

    private static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length == 2 ? decode(parts[1]) : "";
            params.put(key, value);
        }
        return params;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void respondJson(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(bytes);
        }
    }

    private static final class TestHttpServer implements AutoCloseable {
        private final HttpServer server;

        private TestHttpServer(HttpServer server) {
            this.server = server;
        }

        static TestHttpServer start(ThrowingHandler handler) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/", exchange -> {
                try {
                    handler.handle(exchange);
                } finally {
                    exchange.close();
                }
            });
            server.start();
            return new TestHttpServer(server);
        }

        String baseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    @FunctionalInterface
    private interface ThrowingHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
