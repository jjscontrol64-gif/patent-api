package com.jjs.patentapi.service;

import com.jjs.patentapi.client.SerpApiClient;
import com.jjs.patentapi.dto.PatentDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatentDetailService {

    private final SerpApiClient serpApiClient;

    public PatentDetailResponse findById(String id) {
        return serpApiClient.findById(id);
    }
}
