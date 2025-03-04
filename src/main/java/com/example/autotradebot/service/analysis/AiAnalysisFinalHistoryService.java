package com.example.autotradebot.service.analysis;


import com.example.autotradebot.dto.analysis.AiAnalysisFinalHistoryDTO;
import com.example.autotradebot.mapper.analysis.AiAnalysisFinalHistoryMapper;
import com.example.autotradebot.state.ChatGptChartAnalysisCacheManager;
import com.example.autotradebot.state.ChatGptPredictionCacheManager;
import com.example.autotradebot.state.GeminiChartAnalysisCacheManager;
import com.example.autotradebot.state.GeminiPredictionCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiAnalysisFinalHistoryService {

    private final AiAnalysisFinalHistoryMapper aiAnalysisFinalHistoryMapper;

    private ChatGptChartAnalysisCacheManager gptChartAnalysisCacheManager;
    private ChatGptPredictionCacheManager chatGptPredictionCacheManager;
    private GeminiChartAnalysisCacheManager geminiChartAnalysisCacheManager;
    private GeminiPredictionCacheManager geminiPredictionCacheManager;

    @Autowired
    public AiAnalysisFinalHistoryService(AiAnalysisFinalHistoryMapper aiAnalysisFinalHistoryMapper,
                                         ChatGptChartAnalysisCacheManager gptChartAnalysisCacheManager,
                                         ChatGptPredictionCacheManager chatGptPredictionCacheManager,
                                         GeminiChartAnalysisCacheManager geminiChartAnalysisCacheManager,
                                         GeminiPredictionCacheManager geminiPredictionCacheManager
    ) {
        this.aiAnalysisFinalHistoryMapper = aiAnalysisFinalHistoryMapper;
        this.gptChartAnalysisCacheManager = gptChartAnalysisCacheManager;
        this.chatGptPredictionCacheManager = chatGptPredictionCacheManager;
        this.geminiPredictionCacheManager = geminiPredictionCacheManager;
        this.geminiChartAnalysisCacheManager = geminiChartAnalysisCacheManager;
    }

    // ✅ AI 분석 결과 저장
    public void processAiAnalysisFinal(String symbol) {

    }

    // ✅ AI 분석 결과 저장
    public void saveAiAnalysisFinalHistory(AiAnalysisFinalHistoryDTO aiAnalysisFinalHistory) {
        aiAnalysisFinalHistoryMapper.insertAiAnalysisFinalHistory(aiAnalysisFinalHistory);
    }

    // ✅ 특정 심볼의 최신 AI 분석 결과 조회
    public AiAnalysisFinalHistoryDTO getLatestAnalysis(String symbol) {
        return aiAnalysisFinalHistoryMapper.getLatestAiAnalysisFinalHistory(symbol);
    }

    // ✅ 특정 심볼의 전체 AI 분석 결과 조회
    public List<AiAnalysisFinalHistoryDTO> getAllAnalysis(String symbol) {
        return aiAnalysisFinalHistoryMapper.getAllAiAnalysisFinalHistory(symbol);
    }

    // ✅ 특정 심볼의 AI 분석 데이터 삭제
    public void deleteAnalysis(String symbol) {
        aiAnalysisFinalHistoryMapper.deleteAiAnalysisFinalHistory(symbol);
    }
}
