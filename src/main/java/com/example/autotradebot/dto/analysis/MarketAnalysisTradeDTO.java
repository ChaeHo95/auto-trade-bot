package com.example.autotradebot.dto.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * ✅ MarketAnalysis 용 체결된 거래 데이터 DTO
 */
@Data
@Builder
public class MarketAnalysisTradeDTO {
    private BigInteger tradeId; // ✅ 거래 ID
    private BigDecimal price; // ✅ 거래 가격
    private BigDecimal quantity; // ✅ 거래 수량
    private BigInteger tradeTime; // ✅ 거래 시간
    private Boolean isBuyerMaker; // ✅ 매수자=maker 여부
}
