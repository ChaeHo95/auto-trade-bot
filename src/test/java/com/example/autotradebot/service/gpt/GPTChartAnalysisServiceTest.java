package com.example.autotradebot.service.gpt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GPTChartAnalysisServiceTest {

    private GPTChartAnalysisService gptChartAnalysisService;

    @Autowired
    public void setMarketAnalysisService(GPTChartAnalysisService gptChartAnalysisService) {
        this.gptChartAnalysisService = gptChartAnalysisService;
    }

    private static final String SYMBOL = "XRPUSDT";

    @Test
    void scheduledChartAnalysis() {
        gptChartAnalysisService.scheduledChartAnalysis(SYMBOL);
    }
}
