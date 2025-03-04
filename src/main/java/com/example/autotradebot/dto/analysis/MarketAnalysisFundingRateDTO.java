package com.example.autotradebot.dto.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * ✅ MarketAnalysis 용 펀딩 비율 데이터 DTO
 */
@Data
@Builder
public class MarketAnalysisFundingRateDTO {
    private BigInteger fundingTime; // ✅ 펀딩 시간
    private BigDecimal fundingRate; // ✅ 펀딩 비율
    private String symbol; // ✅ 거래 심볼 (BTCUSDT 등)
    private BigDecimal mark_price; // ✅ 거래 심볼 (BTCUSDT 등)
}
