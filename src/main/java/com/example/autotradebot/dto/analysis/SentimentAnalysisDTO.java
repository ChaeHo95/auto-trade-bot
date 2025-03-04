package com.example.autotradebot.dto.analysis;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SentimentAnalysisDTO {
    private String symbol;            // 거래 심볼 (예: BTCUSDT)
    private String position;          // 포지션 (LONG, SHORT, WAIT, EXIT)
    private Double confidence;        // 신뢰도 (0 - 100)
    private String reason;            // 감성 분석 결과 설명
    private String stopLoss;          // 추천 Stop-Loss 가격
    private String takeProfit;        // 추천 Take-Profit 가격
    private String leverage;          // 추천 레버리지 수준
}
