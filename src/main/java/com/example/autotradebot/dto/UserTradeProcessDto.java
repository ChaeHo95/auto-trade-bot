package com.example.autotradebot.dto;

import com.example.autotradebot.enums.OrderState;
import com.example.autotradebot.enums.TradePosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTradeProcessDto {
    private Integer id;               // 사용자 거래 처리 테이블의 고유 ID (PK)
    private String symbol;            // 설정된 심볼
    private BigInteger orderId;       // 주문 고유 번호
    private OrderState orderState;    // 주문 상태 (NEW, FILLED, CANCELED)
    private BigDecimal entryPrice;    // 진입 가격
    private BigDecimal quantity;      // 수량
    private TradePosition position;   // 포지션 (LONG, SHORT, EXIT)
    private BigInteger leverage;      // 레버리지
    private String emailPk;           // 사용자 SNS 고유 식별값 (user_table 참조)
    private Boolean isProcess;        // 처리 여부
    private Date createdAt;         // 생성 일자
}
