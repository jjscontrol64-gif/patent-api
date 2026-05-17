package com.jjs.patentapi.client;

import com.jjs.patentapi.dto.TranslationRequest;
import com.jjs.patentapi.dto.TranslationResponse;
import com.jjs.patentapi.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DeepLClient {

    private final RestClient restClient;
    private final String apiKey;

    public DeepLClient(
            RestClient.Builder restClientBuilder,
            @Value("${external.deepl.key:}") String apiKey,
            @Value("${external.deepl.base-url:https://api-free.deepl.com}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public TranslationResponse translate(TranslationRequest request) {
        if (!hasApiKey()) {
            log.warn("DeepL key not configured — mock fallback");
            return mockTranslation(request);
        }

        try {
            JsonNode root = restClient.post()
                    .uri("/v2/translate")
                    .header("Authorization", "DeepL-Auth-Key " + apiKey)
                    .body(requestBody(request))
                    .retrieve()
                    .body(JsonNode.class);

            return mapTranslationResponse(root);
        } catch (RestClientResponseException ex) {
            throw new ExternalApiException("DeepL translation request failed: HTTP " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new ExternalApiException("DeepL translation request failed.", ex);
        }
    }

    public boolean isConfigured() {
        return hasApiKey();
    }

    private boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    private Map<String, Object> requestBody(TranslationRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", List.of(request.getSourceText()));
        if (!"AUTO".equalsIgnoreCase(request.getSourceLang())) {
            body.put("source_lang", request.getSourceLang());
        }
        body.put("target_lang", request.getTargetLang());
        return body;
    }

    private TranslationResponse mapTranslationResponse(JsonNode root) {
        if (root == null) {
            throw new ExternalApiException("DeepL returned an empty response.");
        }
        JsonNode translations = root.path("translations");
        if (!translations.isArray() || translations.isEmpty()) {
            throw new ExternalApiException("DeepL returned no translations.");
        }
        String translatedText = translations.get(0).path("text").asText("");
        if (translatedText.isBlank()) {
            throw new ExternalApiException("DeepL returned a blank translation.");
        }
        return TranslationResponse.builder()
                .translatedText(translatedText)
                .build();
    }

    private TranslationResponse mockTranslation(TranslationRequest request) {
        return TranslationResponse.builder()
                .translatedText("[번역 미리보기] " + request.getSourceText())
                .build();
    }
}
