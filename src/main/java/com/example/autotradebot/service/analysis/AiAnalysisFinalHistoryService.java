package com.example.autotradebot.service.analysis;


import com.example.autotradebot.dto.analysis.*;
import com.example.autotradebot.mapper.analysis.AiAnalysisFinalHistoryMapper;
import com.example.autotradebot.mapper.analysis.MarketAnalysisMapper;
import com.example.autotradebot.service.gemini.GeminiService;
import com.example.autotradebot.state.ChatGptChartAnalysisCacheManager;
import com.example.autotradebot.state.ChatGptPredictionCacheManager;
import com.example.autotradebot.state.GeminiChartAnalysisCacheManager;
import com.example.autotradebot.state.GeminiPredictionCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiAnalysisFinalHistoryService {

    private final AiAnalysisFinalHistoryMapper aiAnalysisFinalHistoryMapper;

    private ChatGptChartAnalysisCacheManager chatGptChartAnalysisCacheManager;
    private ChatGptPredictionCacheManager chatGptPredictionCacheManager;
    private GeminiChartAnalysisCacheManager geminiChartAnalysisCacheManager;
    private GeminiPredictionCacheManager geminiPredictionCacheManager;

    private GeminiService geminiService;
    private MarketAnalysisService marketAnalysisService;
    private MarketAnalysisMapper marketAnalysisMapper;

    @Autowired
    public AiAnalysisFinalHistoryService(AiAnalysisFinalHistoryMapper aiAnalysisFinalHistoryMapper,
                                         ChatGptChartAnalysisCacheManager chatGptChartAnalysisCacheManager,
                                         ChatGptPredictionCacheManager chatGptPredictionCacheManager,
                                         GeminiChartAnalysisCacheManager geminiChartAnalysisCacheManager,
                                         GeminiPredictionCacheManager geminiPredictionCacheManager,
                                         GeminiService geminiService, MarketAnalysisMapper marketAnalysisMapper
    ) {
        this.aiAnalysisFinalHistoryMapper = aiAnalysisFinalHistoryMapper;
        this.chatGptChartAnalysisCacheManager = chatGptChartAnalysisCacheManager;
        this.chatGptPredictionCacheManager = chatGptPredictionCacheManager;
        this.geminiPredictionCacheManager = geminiPredictionCacheManager;
        this.geminiChartAnalysisCacheManager = geminiChartAnalysisCacheManager;
        this.geminiService = geminiService;
        this.marketAnalysisMapper = marketAnalysisMapper;
    }

    // ✅ AI 분석
    public void processAiAnalysisFinal(String symbol) {
        List<ChartAnalysisDTO> chatGptChartAnalysis = chatGptChartAnalysisCacheManager.getAllChartAnalysis(symbol);
        List<PredictionDTO> chatGptPredictions = chatGptPredictionCacheManager.getAllPredictions(symbol);
        List<ChartAnalysisDTO> geminiChartAnalysis = geminiChartAnalysisCacheManager.getAllChartAnalysis(symbol);
        List<PredictionDTO> geminiPredictions = geminiPredictionCacheManager.getAllPredictions(symbol);


        List<MarketAnalysisKlineDTO> recentKlines = marketAnalysisMapper.getRecentKlines(symbol, 1000)
                .stream()
                .map(kline -> MarketAnalysisKlineDTO.builder()
                        .openTime(kline.getOpenTime())
                        .openPrice(kline.getOpenPrice())
                        .highPrice(kline.getHighPrice())
                        .lowPrice(kline.getLowPrice())
                        .closePrice(kline.getClosePrice())
                        .volume(kline.getVolume())
                        .closeTime(kline.getCloseTime())
                        .build())
                .collect(Collectors.toList());

        List<MarketAnalysisTradeDTO> recentTrades = marketAnalysisMapper.getRecentTrades(symbol, 1000)
                .stream()
                .map(trade -> MarketAnalysisTradeDTO.builder()
                        .price(trade.getPrice())
                        .quantity(trade.getQuantity())
                        .tradeTime(trade.getTradeTime())
                        .isBuyerMaker(trade.getBuyerMaker())
                        .build())
                .collect(Collectors.toList());


        List<MarketAnalysisFundingRateDTO> fundingRates = marketAnalysisMapper.getFundingRates(symbol, 1000)
                .stream()
                .map(fundingRate -> MarketAnalysisFundingRateDTO.builder()
                        .fundingTime(fundingRate.getFundingTime())
                        .fundingRate(fundingRate.getFundingRate())
                        .symbol(fundingRate.getSymbol())
                        .mark_price(fundingRate.getMarkPrice())
                        .build())
                .collect(Collectors.toList());

        BigDecimal movingAverage = marketAnalysisMapper.getMovingAverage(symbol, 1000);
        BigDecimal rsiValue = marketAnalysisMapper.getRSIValue(symbol);
        BigDecimal macdValue = marketAnalysisMapper.getMACDValue(symbol);
        
        String systemMessage = "";
        String userMessage = "";

        String response = geminiService.callGeminiAiApi(systemMessage, userMessage);
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
