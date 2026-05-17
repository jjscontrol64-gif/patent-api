package com.jjs.patentapi.client;

import com.jjs.patentapi.dto.PatentDetailResponse;
import com.jjs.patentapi.dto.PatentItem;
import com.jjs.patentapi.dto.PatentSearchRequest;
import com.jjs.patentapi.dto.PatentSearchResponse;
import com.jjs.patentapi.exception.ExternalApiException;
import com.jjs.patentapi.exception.PatentNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class SerpApiClient {

    private final RestClient restClient;
    private final String apiKey;

    public SerpApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${external.serp-api.key:}") String apiKey,
            @Value("${external.serp-api.base-url:https://serpapi.com}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public PatentSearchResponse search(PatentSearchRequest request) {
        return search(request, request.getQuery());
    }

    public PatentSearchResponse search(PatentSearchRequest request, String searchQuery) {
        if (!hasApiKey()) {
            log.warn("SerpAPI key not configured — mock fallback");
            return mockSearchResponse(request);
        }

        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/search.json")
                                .queryParam("engine", "google_patents")
                                .queryParam("q", searchQuery)
                                .queryParam("country", "JP")
                                .queryParam("page", request.getPage())
                                .queryParam("num", serpApiPageSize(request.getSize()))
                                .queryParam("api_key", apiKey);
                        if (hasText(request.getFrom())) {
                            builder.queryParam("after", filingDateFilter(request.getFrom()));
                        }
                        if (hasText(request.getTo())) {
                            builder.queryParam("before", filingDateFilter(request.getTo()));
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);

            assertNoSerpApiError(root);
            return mapSearchResponse(request, root);
        } catch (RestClientResponseException ex) {
            throw new ExternalApiException("SerpAPI search request failed: HTTP " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new ExternalApiException("SerpAPI search request failed.", ex);
        }
    }

    public PatentDetailResponse findById(String id) {
        if (!hasApiKey()) {
            log.warn("SerpAPI key not configured — mock fallback");
            return mockDetailResponse(id);
        }

        try {
            String patentId = normalizePatentId(id);
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/search.json")
                            .queryParam("engine", "google_patents_details")
                            .queryParam("patent_id", patentId)
                            .queryParam("api_key", apiKey)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            assertNoSerpApiError(root);
            return mapDetailResponse(id, root);
        } catch (PatentNotFoundException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw new ExternalApiException("SerpAPI detail request failed: HTTP " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new ExternalApiException("SerpAPI detail request failed.", ex);
        }
    }

    private boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean isConfigured() {
        return hasApiKey();
    }

    private PatentSearchResponse mapSearchResponse(PatentSearchRequest request, JsonNode root) {
        List<PatentItem> items = new ArrayList<>();
        JsonNode organicResults = root.path("organic_results");
        if (organicResults.isArray()) {
            for (JsonNode result : organicResults) {
                if (items.size() >= request.getSize()) {
                    break;
                }
                String publicationNumber = text(result, "publication_number");
                if (!isJapanesePublication(publicationNumber)) {
                    continue;
                }
                String patentId = text(result, "patent_id");
                items.add(PatentItem.builder()
                        .id(hasText(publicationNumber) ? publicationNumber : patentId)
                        .title(text(result, "title"))
                        .applicant(text(result, "assignee"))
                        .filingDate(text(result, "filing_date"))
                        .publicationNumber(publicationNumber)
                        .build());
            }
        }

        return PatentSearchResponse.builder()
                .query(request.getQuery())
                .page(request.getPage())
                .size(request.getSize())
                .totalCount(totalResults(root))
                .items(items)
                .build();
    }

    private PatentDetailResponse mapDetailResponse(String requestedId, JsonNode root) {
        String title = text(root, "title");
        String publicationNumber = text(root, "publication_number");
        if (!hasText(title) && !hasText(publicationNumber)) {
            throw new PatentNotFoundException(requestedId);
        }

        return PatentDetailResponse.builder()
                .id(hasText(publicationNumber) ? publicationNumber : requestedId)
                .title(title)
                .abstractText(text(root, "abstract"))
                .applicant(joinTextArray(root.path("assignees")))
                .filingDate(text(root, "filing_date"))
                .publicationNumber(publicationNumber)
                .originalUrl(text(root.path("search_metadata"), "google_patents_details_url"))
                .build();
    }

    private void assertNoSerpApiError(JsonNode root) {
        if (root == null) {
            throw new ExternalApiException("SerpAPI returned an empty response.");
        }
        String error = text(root, "error");
        if (hasText(error)) {
            throw new ExternalApiException("SerpAPI returned an error: " + error);
        }
    }

    private int totalResults(JsonNode root) {
        long total = root.path("search_information").path("total_results").asLong(0);
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private int serpApiPageSize(int requestedSize) {
        return Math.max(10, Math.min(100, requestedSize));
    }

    private String filingDateFilter(String value) {
        if (value.contains(":")) {
            return value;
        }
        return "filing:" + value.replace("-", "");
    }

    private String normalizePatentId(String id) {
        if (id.startsWith("patent/") || id.startsWith("scholar/")) {
            return id;
        }
        return "patent/" + id;
    }

    private String joinTextArray(JsonNode node) {
        if (!node.isArray()) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual() && hasText(item.asText())) {
                values.add(item.asText());
            }
        }
        return String.join(", ", values);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isJapanesePublication(String publicationNumber) {
        return hasText(publicationNumber) && publicationNumber.toUpperCase().startsWith("JP");
    }

    private PatentSearchResponse mockSearchResponse(PatentSearchRequest request) {
        List<PatentItem> items = List.of(
                PatentItem.builder()
                        .id("JP2024000001")
                        .title("人工知能を用いた特許検索システム")
                        .applicant("Example Corp")
                        .filingDate("2024-01-10")
                        .publicationNumber("JP2024-000001")
                        .build(),
                PatentItem.builder()
                        .id("JP2023000042")
                        .title("機械学習による文書分類方法")
                        .applicant("Tech Institute")
                        .filingDate("2023-07-22")
                        .publicationNumber("JP2023-000042")
                        .build()
        );
        return PatentSearchResponse.builder()
                .query(request.getQuery())
                .page(request.getPage())
                .size(request.getSize())
                .totalCount(items.size())
                .items(items)
                .build();
    }

    private PatentDetailResponse mockDetailResponse(String id) {
        return PatentDetailResponse.builder()
                .id(id)
                .title("人工知能を用いた特許検索システム")
                .abstractText("本発明は、人工知能技術を活用した革新的な特許検索システムに関するものである。")
                .applicant("Example Corp")
                .filingDate("2024-01-10")
                .publicationNumber("JP2024-000001")
                .originalUrl("https://patents.google.com/patent/" + id)
                .build();
    }
}
