package com.example.autotradebot.scheduler;

import com.example.autotradebot.dto.analysis.PredictionDTO;
import com.example.autotradebot.service.analysis.AiAnalysisFinalHistoryService;
import com.example.autotradebot.state.ChatGptPredictionCacheManager;
import com.example.autotradebot.state.GeminiPredictionCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


@Component
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
    @Scheduled(cron = "0 * * * * ?") // ⏰ 매 1분 실행
    public void processAiFinalAnalysis() {

        for (String symbol : symbols) {
            PredictionDTO latestGeminiPrediction = geminiPredictionCacheManager.getLatestPrediction(symbol);
            PredictionDTO latestGptPrediction = chatGptPredictionCacheManager.getLatestPrediction(symbol);

            PredictionDTO oldGeminiPrediction = geminiPredictionCache.get(symbol);
            PredictionDTO oldGptPrediction = chatGptPredictionCache.get(symbol);

            if (oldGeminiPrediction == null && oldGptPrediction == null && latestGeminiPrediction != null && latestGptPrediction != null) {
                processStart(symbol);
            } else {
                if (oldGeminiPrediction == null && oldGptPrediction == null && latestGeminiPrediction == null && latestGptPrediction == null) {
                    return;
                }

                if (latestGeminiPrediction.getAnalysisTime().isAfter(oldGeminiPrediction.getAnalysisTime()) || latestGptPrediction.getAnalysisTime().isAfter(oldGptPrediction.getAnalysisTime())) {
                    processStart(symbol);
                }
            }
        }
    }

    private void processStart(String symbol) {
        logger.info("🔄 AI 최종 분석 시작...{}", symbol);
        aiAnalysisFinalHistoryService.processAiAnalysisFinal(symbol);

        geminiPredictionCache.put(symbol, geminiPredictionCacheManager.getLatestPrediction(symbol));
        chatGptPredictionCache.put(symbol, chatGptPredictionCacheManager.getLatestPrediction(symbol));

        logger.info("✅ AI 최종 분석 완료: {}", symbol);
    }
}
