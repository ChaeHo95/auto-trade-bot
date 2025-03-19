package com.example.autotradebot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeSignalDto {
    // 거래 종목 (예: "XRPUSDT")
    private String symbol;
    // 포지션 진입 가격
    private BigDecimal entryPrice;
    // 현재 포지션
    private String position;
    // 현재 레버리지
    private int leverage;
}
