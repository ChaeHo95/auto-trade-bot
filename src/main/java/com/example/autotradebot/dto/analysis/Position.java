package com.example.autotradebot.dto.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Position {
    private String symbol;            // 거래 심볼 (예: BTCUSDT)
    private String positionStatus;    // "LONG", "SHORT", "WAIT", "EXIT" 등
    private BigDecimal entryPrice;    // 진입 가격
    private BigDecimal quantity;      // 포지션 수량 (예: 1 BTC)
    private LocalDateTime entryTime;  // 포지션 진입 시간
    private BigDecimal leverage;      // 레버리지

    // 생성자
    public Position(String symbol, String positionStatus, BigDecimal entryPrice, BigDecimal quantity, LocalDateTime entryTime, BigDecimal leverage) {
        this.symbol = symbol;
        this.positionStatus = positionStatus;
        this.entryPrice = entryPrice;
        this.quantity = quantity;
        this.entryTime = entryTime;
        this.leverage = leverage;
    }

    // getter 및 setter
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getPositionStatus() {
        return positionStatus;
    }

    public void setPositionStatus(String positionStatus) {
        this.positionStatus = positionStatus;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public BigDecimal getLeverage() {
        return leverage;
    }

    public void setLeverage(BigDecimal leverage) {
        this.leverage = leverage;
    }

    // 포지션 상태 변경 메소드
    public void updatePositionStatus(String newStatus) {
        this.positionStatus = newStatus;
    }

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);  // Position 객체를 JSON 문자열로 변환
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}";  // 변환에 실패한 경우 빈 JSON 반환
        }
    }
}
