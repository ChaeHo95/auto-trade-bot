package com.example.autotradebot.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
public class AiAnalysisFinalHistoryDTO {
    private BigInteger id;
    private String symbol;
    private LocalDateTime analysisTime;
    private String recommendedPosition;
    private String executedPosition;
    private BigDecimal profitLoss;
    private BigDecimal confidenceScore;
    private String reason; // 분석 이유
}
