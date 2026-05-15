package com.jjs.patentapi.controller;

import com.jjs.patentapi.dto.PatentDetailResponse;
import com.jjs.patentapi.dto.PatentSearchRequest;
import com.jjs.patentapi.dto.PatentSearchResponse;
import com.jjs.patentapi.service.PatentDetailService;
import com.jjs.patentapi.service.PatentSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patents")
@RequiredArgsConstructor
public class PatentController {

    private final PatentSearchService searchService;
    private final PatentDetailService detailService;

    @GetMapping("/search")
    public PatentSearchResponse search(@Valid @ModelAttribute PatentSearchRequest request) {
        return searchService.search(request);
    }

    @GetMapping("/{id}")
    public PatentDetailResponse detail(@PathVariable String id) {
        return detailService.findById(id);
    }
}
