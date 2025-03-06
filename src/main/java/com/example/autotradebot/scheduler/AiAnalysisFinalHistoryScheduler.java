package com.example.autotradebot.scheduler;

import com.example.autotradebot.dto.analysis.PredictionDTO;
import com.example.autotradebot.service.analysis.AiAnalysisFinalHistoryService;
import com.example.autotradebot.state.ChatGptPredictionCacheManager;
import com.example.autotradebot.state.GeminiPredictionCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "enable.ai.scheduling", havingValue = "true", matchIfMissing = false)
public class AiAnalysisFinalHistoryScheduler {
    private Logger logger = LoggerFactory.getLogger(AiAnalysisFinalHistoryScheduler.class);

    private final AiAnalysisFinalHistoryService aiAnalysisFinalHistoryService;

    private ChatGptPredictionCacheManager chatGptPredictionCacheManager;
    private GeminiPredictionCacheManager geminiPredictionCacheManager;

    private final ConcurrentHashMap<String, PredictionDTO> chatGptPredictionCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PredictionDTO> geminiPredictionCache = new ConcurrentHashMap<>();


    @Value("${symbols}")
    private List<String> symbols;

    @Autowired
    public AiAnalysisFinalHistoryScheduler(AiAnalysisFinalHistoryService aiAnalysisFinalHistoryService,
                                           ChatGptPredictionCacheManager chatGptPredictionCacheManager,
                                           GeminiPredictionCacheManager geminiPredictionCacheManager
    ) {
        this.aiAnalysisFinalHistoryService = aiAnalysisFinalHistoryService;
        this.chatGptPredictionCacheManager = chatGptPredictionCacheManager;
        this.geminiPredictionCacheManager = geminiPredictionCacheManager;
    }

    /**
     * ✅ 매 1분마다 AI 최종 분석 수행 및 저장
     */
    @Scheduled(fixedRate = 60000, initialDelay = 300000)    // 1분마다 실행 (60,000ms = 1분)
    public void processAiFinalAnalysis() {
        logger.info("AI 최종 분석 프로세스 시작 {}", symbols);

        for (String symbol : symbols) {
            logger.info("처리 중인 심볼: {}", symbol);

            PredictionDTO latestGeminiPrediction = geminiPredictionCacheManager.getLatestPrediction(symbol);
            PredictionDTO latestGptPrediction = chatGptPredictionCacheManager.getLatestPrediction(symbol);

            PredictionDTO oldGeminiPrediction = geminiPredictionCache.get(symbol);
            PredictionDTO oldGptPrediction = chatGptPredictionCache.get(symbol);

            // 로그 추가: 최근 예측 값과 이전 예측 값
            logger.info("latestGeminiPrediction: {}", latestGeminiPrediction);
            logger.info("latestGptPrediction: {}", latestGptPrediction);
            logger.info("oldGeminiPrediction: {}", oldGeminiPrediction);
            logger.info("oldGptPrediction: {}", oldGptPrediction);

            if (oldGeminiPrediction == null && oldGptPrediction == null && latestGeminiPrediction != null && latestGptPrediction != null) {
                logger.info("둘 다 이전 예측 값이 없고, 최신 예측 값이 있는 경우 - processStart 호출");
                processStart(symbol);
            } else {
                if (oldGeminiPrediction == null && oldGptPrediction == null && latestGeminiPrediction == null && latestGptPrediction == null) {
                    logger.info("둘 다 이전 예측 값과 최신 예측 값이 없는 경우 - 종료");
                    return;
                }

                if (latestGeminiPrediction.getAnalysisTime().isAfter(oldGeminiPrediction.getAnalysisTime()) || latestGptPrediction.getAnalysisTime().isAfter(oldGptPrediction.getAnalysisTime())) {
                    logger.info("최신 예측 값이 이전 예측 값보다 나은 경우 - processStart 호출");
                    processStart(symbol);
                }
            }
        }

        logger.info("AI 최종 분석 프로세스 종료");
    }


    private void processStart(String symbol) {
        logger.info("🔄 AI 최종 분석 시작...{}", symbol);
        aiAnalysisFinalHistoryService.processAiAnalysisFinal(symbol);

        geminiPredictionCache.put(symbol, geminiPredictionCacheManager.getLatestPrediction(symbol));
        chatGptPredictionCache.put(symbol, chatGptPredictionCacheManager.getLatestPrediction(symbol));

        logger.info("✅ AI 최종 분석 완료: {}", symbol);
    }
}
