package com.jjs.patentapi.controller;

import com.jjs.patentapi.dto.TranslationRequest;
import com.jjs.patentapi.dto.TranslationResponse;
import com.jjs.patentapi.service.TranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/translations")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService translationService;

    @PostMapping
    public TranslationResponse translate(@Valid @RequestBody TranslationRequest request) {
        return translationService.translate(request);
    }
}
