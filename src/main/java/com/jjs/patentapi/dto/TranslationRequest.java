package com.jjs.patentapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TranslationRequest {

    @NotBlank(message = "sourceText를 입력하세요")
    private String sourceText;

    @NotBlank(message = "sourceLang을 입력하세요")
    private String sourceLang;

    @NotBlank(message = "targetLang을 입력하세요")
    private String targetLang;
}
