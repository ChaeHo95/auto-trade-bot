package com.example.autotradebot.service.gemini;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GeminiServiceTest {

    private Logger logger = LoggerFactory.getLogger(GeminiServiceTest.class);

    private GeminiService geminiService;

    @Autowired
    public void setMarketAnalysisService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    private static final String SYMBOL = "XRPUSDT";


    @Test
    void requestGemini() {
        String result = geminiService.requestGemini(SYMBOL);
        logger.info(result);
    }
}
