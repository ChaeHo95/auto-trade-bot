package com.example.autotradebot.dto.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * ✅ AI 분석을 위한 종합 데이터 DTO
 */
@Data
@Builder
public class MarketAnalysisDTO {
    private String symbol; // 거래 심볼 (BTCUSDT)

    private List<MarketAnalysisKlineDTO> recentKlines;  // ✅ 최근 200개 캔들 데이터
    private List<MarketAnalysisTradeDTO> recentTrades;  // ✅ 최근 100개 체결 거래 데이터
    private List<MarketAnalysisFundingRateDTO> fundingRates; // ✅ 최근 20개 펀딩 비율 데이터
    private ChartAnalysisPredictionDTO currentPosition; // ✅ AI 분석용 차트 데이터 봇

    private BigDecimal movingAverage; // ✅ 이동평균선 (200개 캔들 기준)
    private BigDecimal rsiValue; // ✅ RSI 값
    private BigDecimal macdValue; // ✅ MACD 값
}
