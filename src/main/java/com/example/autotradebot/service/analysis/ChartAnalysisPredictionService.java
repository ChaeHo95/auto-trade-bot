package com.example.autotradebot.service.analysis;

import com.example.autotradebot.dto.analysis.ChartAnalysisPredictionDTO;
import com.example.autotradebot.mapper.analysis.ChartAnalysisPredictionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChartAnalysisPredictionService {

    private final ChartAnalysisPredictionMapper predictionMapper;

    @Autowired
    public ChartAnalysisPredictionService(ChartAnalysisPredictionMapper predictionMapper) {
        this.predictionMapper = predictionMapper;
    }

    /**
     * ✅ 특정 심볼의 최신 AI 차트 분석 결과 가져오기
     */
    public ChartAnalysisPredictionDTO getLatestPrediction(String symbol) {
        return predictionMapper.getLatestPrediction(symbol);
    }

    /**
     * ✅ AI 분석 결과 저장
     */
    public void savePrediction(ChartAnalysisPredictionDTO prediction) {
        predictionMapper.savePrediction(prediction);
    }
}
