package com.example.autotradebot.dto;

import com.example.autotradebot.enums.TradePosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeSignalDto {
    private String symbol;              // 거래 종목 (예: "XRPUSDT")
    private BigDecimal entryPrice;      // 포지션 진입 가격
    private TradePosition position;     // 현재 포지션
    private BigInteger leverage;        // 현재 레버리지
}
