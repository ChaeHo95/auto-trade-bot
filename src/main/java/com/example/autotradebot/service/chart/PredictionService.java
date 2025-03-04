package com.example.autotradebot.service.chart;

import com.example.autotradebot.dto.analysis.PredictionDTO;
import com.example.autotradebot.mapper.analysis.PredictionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PredictionService {

    private final PredictionMapper predictionMapper;

    @Autowired
    public PredictionService(PredictionMapper predictionMapper) {
        this.predictionMapper = predictionMapper;
    }

    /**
     * ✅ 특정 심볼의 최신 AI 차트 분석 결과 가져오기
     */
    public PredictionDTO getLatestPrediction(String symbol) {
        return predictionMapper.getLatestPrediction(symbol);
    }

    /**
     * ✅ AI 분석 결과 저장
     */
    public void savePrediction(String bot, PredictionDTO prediction) {
        predictionMapper.savePrediction(bot, prediction);
    }
}
