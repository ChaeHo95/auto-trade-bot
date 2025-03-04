package com.example.autotradebot.dto.analysis;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

/**
 * ✅ AI가 분석한 시장 감성 분석 결과 DTO
 */
@Data
@Builder
public class MarketSentimentAnalysisDTO {

    private Long id;  // ✅ 시장 감성 분석 결과 ID
    private String symbol;  // ✅ 거래 심볼 (예: BTCUSDT)
    private LocalDateTime analysisTime;  // ✅ 분석 수행 시각
    private BigDecimal sentimentScore;  // ✅ 감성 점수 (-100 ~ 100, 부정적 ~ 긍정적)
    private BigInteger positiveNewsCount;  // ✅ 긍정적 뉴스 개수
    private BigInteger negativeNewsCount;  // ✅ 부정적 뉴스 개수
    private BigInteger neutralNewsCount;  // ✅ 중립 뉴스 개수
    private BigInteger tweetVolume;  // ✅ 트윗 개수
    private String economicEvent;  // ✅ 주요 경제 이벤트 (예: FOMC 발표 등)
    private LocalDateTime createdAt;  // ✅ 데이터 생성 시각
}
