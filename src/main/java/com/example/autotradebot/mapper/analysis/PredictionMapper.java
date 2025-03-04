package com.example.autotradebot.mapper.analysis;

import com.example.autotradebot.dto.analysis.PredictionDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PredictionMapper {

    // ✅ 최신 AI 분석 결과 가져오기
    PredictionDTO getLatestPrediction(@Param("symbol") String symbol);

    // ✅ AI 분석 결과 저장
    void savePrediction(PredictionDTO prediction);
}
