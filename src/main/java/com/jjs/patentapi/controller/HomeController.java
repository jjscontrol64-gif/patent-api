package com.jjs.patentapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "service", "patent-api",
                "status", "running",
                "endpoints", List.of(
                        "GET /actuator/health",
                        "GET /api/patents/search?query={query}",
                        "GET /api/patents/{id}",
                        "POST /api/translations"
                )
        );
    }
}
