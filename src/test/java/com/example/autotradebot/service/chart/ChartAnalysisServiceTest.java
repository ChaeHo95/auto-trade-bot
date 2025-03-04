package com.example.autotradebot.service.chart;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ChartAnalysisServiceTest {

    private ChartAnalysisService chartAnalysisService;

    @Autowired
    public void setChartAnalysisService(ChartAnalysisService chartAnalysisService) {
        this.chartAnalysisService = chartAnalysisService;
    }

    @Test
    void scheduledChartAnalysis() {
        chartAnalysisService.scheduledChartAnalysis("XRPUSDT", "GEMINI");
        chartAnalysisService.scheduledChartAnalysis("XRPUSDT", "CHATGPT");
    }
}
