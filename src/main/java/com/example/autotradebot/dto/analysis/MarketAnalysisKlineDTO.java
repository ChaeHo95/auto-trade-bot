package com.example.autotradebot.dto.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * ✅ MarketAnalysis 용 Kline 데이터 DTO
 */
@Data
@Builder
public class MarketAnalysisKlineDTO {
    private BigInteger openTime; // ✅ 캔들 시작 시간
    private BigDecimal openPrice; // ✅ 시가
    private BigDecimal highPrice; // ✅ 고가
    private BigDecimal lowPrice; // ✅ 저가
    private BigDecimal closePrice; // ✅ 종가
    private BigDecimal volume; // ✅ 거래량
    private BigInteger closeTime; // ✅ 캔들 종료 시간
}
