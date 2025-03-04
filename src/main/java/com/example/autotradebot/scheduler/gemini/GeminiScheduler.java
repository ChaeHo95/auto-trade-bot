package com.example.autotradebot.scheduler.gemini;

import com.example.autotradebot.service.gemini.GeminiAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GeminiScheduler {

    private Logger logger = LoggerFactory.getLogger(GeminiScheduler.class);
    private final GeminiAnalysisService geminiAnalysisService;

    private final int minute = 60 * 1000;
    private final String[] coins = {"BITCOIN", "ETHEREUM", "SOLANA", "XRP"};

    public GeminiScheduler(GeminiAnalysisService geminiAnalysisService) {
        this.geminiAnalysisService = geminiAnalysisService;
    }

//    /**
//     * 시장 심리 분석 (5분마다 실행, 비동기 처리)
//     */
//    @Scheduled(fixedDelay = 5 * minute)
//    @Async
//    public void analyzeMarketSentiment() {
//        logger.info("🚀 시장 심리 분석 시작...");
//
//
//        for (String coin : coins) {
//            String sentiment = geminiAnalysisService.analyzeMarketSentiment(coin);
//            logger.info("📊 {} 시장 심리 분석 결과: {}", coin, sentiment);
//        }
//        logger.info("✅ 시장 심리 분석 완료!");
//    }
//
//    /**
//     * 가격 예측 (1분마다 실행, 비동기 처리)
//     */
//    @Scheduled(fixedDelay = 1 * minute)
//    @Async
//    public void predictPrices() {
//        logger.info("🚀 가격 예측 시작...");
//
//        for (String coin : coins) {
//            String prediction = geminiAnalysisService.predictPrice(coin, "1주");
//            logger.info("📈 {} 가격 예측: {}", coin, prediction);
//        }
//        logger.info("✅ 가격 예측 완료!");
//    }
}
