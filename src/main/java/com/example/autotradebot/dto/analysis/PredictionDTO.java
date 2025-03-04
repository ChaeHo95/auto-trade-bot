package com.example.autotradebot.dto.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ✅ AI가 분석한 차트 예측 결과 DTO
 */
@Data
@Builder
public class PredictionDTO {
    private Long id; // ✅ 차트 분석 결과 ID
    private String symbol; // ✅ 거래 심볼 (예: BTCUSDT)
    private LocalDateTime analysisTime; // ✅ 분석 수행 시각
    private String recommendedPosition; // ✅ AI 예측 포지션 (LONG, SHORT, EXIT, WAIT)
    private BigDecimal confidenceScore; // ✅ AI 예측 신뢰도 (%)
    private BigDecimal movingAverage; // ✅ 이동평균선 (200개 캔들 기준)
    private BigDecimal rsiValue; // ✅ RSI 값
    private BigDecimal macdValue; // ✅ MACD 값
    private BigDecimal volatility; // ✅ 변동성 (최근 캔들 기준)
    private BigDecimal fundingRate; // ✅ 최근 펀딩 비율
    private BigDecimal tradeVolume; // ✅ 거래량
    private LocalDateTime createdAt; // ✅ 데이터 생성 시각
    private String reason; // ✅ AI 분석의 이유 및 설명
}
