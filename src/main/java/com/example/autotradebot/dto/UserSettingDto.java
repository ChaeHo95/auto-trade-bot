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
public class UserSettingDto {
    // user 고유 값
    private String emailPk;
    // 거래 종목 (예: "XRPUSDT")
    private String symbol;
    // 거래 금액
    private BigDecimal amount;
    // Access Key
    private String accessKey;
    // Secret Key
    private String secretKey;
}
