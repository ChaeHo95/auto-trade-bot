package com.example.autotradebot.service.gemini;

import org.springframework.stereotype.Service;

@Service
public class GeminiAnalysisService {

    private final GeminiService geminiService;

    public GeminiAnalysisService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }
}
