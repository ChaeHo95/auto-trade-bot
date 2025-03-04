package com.example.autotradebot.service.analysis;

import com.example.autotradebot.dto.analysis.MarketAnalysisDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class MarketAnalysisServiceTest {


    private MarketAnalysisService marketAnalysisService;

    @Autowired
    public void setMarketAnalysisService(MarketAnalysisService marketAnalysisService) {
        this.marketAnalysisService = marketAnalysisService;
    }

    private static final String SYMBOL = "XRPUSDT";

    @Test
    void getMarketAnalysis_shouldReturnValidData() {
        // ✅ 실제 DB에서 데이터 가져오기
        MarketAnalysisDTO marketAnalysisDTO = marketAnalysisService.getMarketAnalysis(SYMBOL);
        // ✅ 검증
        assertNotNull(marketAnalysisDTO, "MarketAnalysisDTO should not be null");
        assertEquals(SYMBOL, marketAnalysisDTO.getSymbol(), "Symbol should match the requested value");

        // ✅ 데이터 출력
        System.out.println("marketAnalysisDTO = " + marketAnalysisDTO);
    }
}
