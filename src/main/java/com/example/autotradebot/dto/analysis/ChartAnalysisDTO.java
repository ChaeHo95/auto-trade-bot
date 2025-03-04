package com.example.autotradebot.dto.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ChartAnalysisDTO {
    private String symbol; // 거래 심볼
    private String position; // 포지션 (LONG, SHORT, EXIT, WAIT)
    private BigDecimal confidence; // 예측 신뢰도
    private String reason; // 분석 이유
    private String stopLoss; // 추천 Stop-Loss
    private String takeProfit; // 추천 Take-Profit
    private String leverage; // 추천 Leverage
}
