package com.jjs.patentapi.service;

import com.jjs.patentapi.client.DeepLClient;
import com.jjs.patentapi.dto.TranslationRequest;
import com.jjs.patentapi.dto.TranslationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private final DeepLClient deepLClient;

    public TranslationResponse translate(TranslationRequest request) {
        return deepLClient.translate(request);
    }
}
