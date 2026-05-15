package com.jjs.patentapi.service;

import com.jjs.patentapi.client.SerpApiClient;
import com.jjs.patentapi.dto.PatentSearchRequest;
import com.jjs.patentapi.dto.PatentSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatentSearchService {

    private final SerpApiClient serpApiClient;

    public PatentSearchResponse search(PatentSearchRequest request) {
        return serpApiClient.search(request);
    }
}
