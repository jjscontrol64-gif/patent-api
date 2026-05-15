package com.jjs.patentapi.client;

import com.jjs.patentapi.dto.PatentDetailResponse;
import com.jjs.patentapi.dto.PatentItem;
import com.jjs.patentapi.dto.PatentSearchRequest;
import com.jjs.patentapi.dto.PatentSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class SerpApiClient {

    @Value("${external.serp-api.key:}")
    private String apiKey;

    public PatentSearchResponse search(PatentSearchRequest request) {
        if (!hasApiKey()) {
            log.warn("SerpAPI key not configured — mock fallback");
            return mockSearchResponse(request);
        }
        // TODO: 실제 SerpAPI 호출
        return mockSearchResponse(request);
    }

    public PatentDetailResponse findById(String id) {
        if (!hasApiKey()) {
            log.warn("SerpAPI key not configured — mock fallback");
            return mockDetailResponse(id);
        }
        // TODO: 실제 SerpAPI 호출
        return mockDetailResponse(id);
    }

    private boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
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
