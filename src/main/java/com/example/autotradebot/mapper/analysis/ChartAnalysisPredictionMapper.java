package com.example.autotradebot.mapper.analysis;

import com.example.autotradebot.dto.analysis.ChartAnalysisPredictionDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChartAnalysisPredictionMapper {

    // ✅ 최신 AI 분석 결과 가져오기
    ChartAnalysisPredictionDTO getLatestPrediction(@Param("symbol") String symbol);

    // ✅ AI 분석 결과 저장
    void savePrediction(ChartAnalysisPredictionDTO prediction);
}
