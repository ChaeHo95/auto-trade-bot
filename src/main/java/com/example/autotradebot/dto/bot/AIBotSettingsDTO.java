package com.example.autotradebot.dto.bot;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ✅ AI 봇의 현재 설정 & 포지션 DTO
 */
@Data
public class AIBotSettingsDTO {
    private Long id; // AI 봇 ID
    private Long apiKeyId; // 연결된 API 키 ID (vendor_api_keys 참조)
    private String symbol; // 거래할 코인 (예: BTC, ETH 등)

    private BigDecimal symbolBalance; // 현재 보유 코인 수량 (BTC, ETH 등)
    private BigDecimal initialQuantity; // 초기 거래 금액 (USDT 기준)
    private Integer leverage; // 레버리지
    private BigDecimal currentQuantity; // 현재 거래 금액 (USDT 기준, 업데이트됨)

    private String position; // 현재 보유 포지션 (LONG, SHORT, WAIT, NONE)
    private String status; // 봇 상태 (active, paused, stopped)
    private String statusChangeReason; // 상태 변경 사유

    private LocalDateTime createdDt; // 생성 날짜
    private LocalDateTime modifiedDt; // 최근 수정 날짜
}
