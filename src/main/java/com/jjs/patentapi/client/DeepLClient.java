package com.jjs.patentapi.client;

import com.jjs.patentapi.dto.TranslationRequest;
import com.jjs.patentapi.dto.TranslationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeepLClient {

    @Value("${external.deepl.key:}")
    private String apiKey;

    public TranslationResponse translate(TranslationRequest request) {
        if (!hasApiKey()) {
            log.warn("DeepL key not configured — mock fallback");
            return mockTranslation(request);
        }
        // TODO: 실제 DeepL API 호출
        return mockTranslation(request);
    }

    private boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    private TranslationResponse mockTranslation(TranslationRequest request) {
        return TranslationResponse.builder()
                .translatedText("[번역 미리보기] " + request.getSourceText())
                .build();
    }
}
