package com.example.autotradebot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPositionHistoryDto {
    private Long id;
    private String emailPk;
    private String symbol;
    private BigDecimal entryPrice;
    private BigDecimal quantity;
    private String position;
    private Integer leverage;
    private Date createdAt;
}
