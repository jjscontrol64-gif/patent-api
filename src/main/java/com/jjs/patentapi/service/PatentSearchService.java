package com.jjs.patentapi.service;

import com.jjs.patentapi.client.DeepLClient;
import com.jjs.patentapi.client.SerpApiClient;
import com.jjs.patentapi.dto.PatentSearchRequest;
import com.jjs.patentapi.dto.PatentSearchResponse;
import com.jjs.patentapi.dto.TranslationRequest;
import com.jjs.patentapi.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatentSearchService {

    private final SerpApiClient serpApiClient;
    private final DeepLClient deepLClient;

    public PatentSearchResponse search(PatentSearchRequest request) {
        String searchQuery = prepareSearchQuery(request.getQuery());
        return serpApiClient.search(request, searchQuery);
    }

    private String prepareSearchQuery(String query) {
        if (!containsKorean(query)) {
            return query;
        }
        if (!serpApiClient.isConfigured()) {
            return query;
        }
        if (!deepLClient.isConfigured()) {
            throw new ExternalApiException("DeepL key is required to translate Korean search terms before SerpAPI search.");
        }

        TranslationRequest translationRequest = new TranslationRequest();
        translationRequest.setSourceText(query);
        translationRequest.setSourceLang("KO");
        translationRequest.setTargetLang("JA");

        String translatedQuery = deepLClient.translate(translationRequest).getTranslatedText();
        log.debug("Translated Korean patent search query to Japanese for SerpAPI.");
        return translatedQuery;
    }

    private boolean containsKorean(String value) {
        return value != null && value.codePoints()
                .anyMatch(codePoint -> codePoint >= 0xAC00 && codePoint <= 0xD7A3);
    }
}
