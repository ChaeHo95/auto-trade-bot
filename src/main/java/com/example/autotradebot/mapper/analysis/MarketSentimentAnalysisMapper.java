package com.example.autotradebot.mapper.analysis;

import com.example.autotradebot.dto.analysis.MarketSentimentAnalysisDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MarketSentimentAnalysisMapper {

    // ✅ 특정 심볼의 최신 시장 감성 분석 결과 가져오기
    MarketSentimentAnalysisDTO getLatestSentimentAnalysis(@Param("symbol") String symbol);

    // ✅ 시장 감성 분석 결과 저장
    void saveSentimentAnalysis(MarketSentimentAnalysisDTO sentimentAnalysisDTO);
}
