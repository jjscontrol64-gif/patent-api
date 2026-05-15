package com.jjs.patentapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TranslationResponse {
    private String translatedText;
}
