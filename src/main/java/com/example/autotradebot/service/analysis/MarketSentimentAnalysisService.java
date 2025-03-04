package com.example.autotradebot.service.analysis;

import com.example.autotradebot.dto.analysis.MarketSentimentAnalysisDTO;
import com.example.autotradebot.mapper.analysis.MarketSentimentAnalysisMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketSentimentAnalysisService {

    private final MarketSentimentAnalysisMapper sentimentAnalysisMapper;

    @Autowired
    public MarketSentimentAnalysisService(MarketSentimentAnalysisMapper sentimentAnalysisMapper) {
        this.sentimentAnalysisMapper = sentimentAnalysisMapper;
    }

    /**
     * ✅ 특정 심볼의 최신 시장 감성 분석 결과 가져오기
     */
    public MarketSentimentAnalysisDTO getLatestSentimentAnalysis(String symbol) {
        return sentimentAnalysisMapper.getLatestSentimentAnalysis(symbol);
    }

    /**
     * ✅ 시장 감성 분석 결과 저장
     */
    public void saveSentimentAnalysis(MarketSentimentAnalysisDTO sentimentAnalysisDTO) {
        sentimentAnalysisMapper.saveSentimentAnalysis(sentimentAnalysisDTO);
    }
}
