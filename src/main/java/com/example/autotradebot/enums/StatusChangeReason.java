package com.example.autotradebot.enums;

/**
 * ✅ AI 봇 상태 변경 사유 ENUM
 */
public enum StatusChangeReason {
    SYMBOL_BALANCE_INSUFFICIENT,
    INVESTMENT_AMOUNT_INSUFFICIENT,
    USER_CHANGED,
    SUBSCRIPTION_EXPIRED,
    NO_AUTHORIZATION_IP;
}
