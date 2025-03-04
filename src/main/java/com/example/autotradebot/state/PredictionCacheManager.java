package com.example.autotradebot.state;

import com.example.autotradebot.dto.analysis.ChartAnalysisPredictionDTO;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class PredictionCacheManager {

    // ConcurrentHashMap을 사용하여 멀티 스레드 환경에서도 안전하게 상태 관리
    private final ConcurrentHashMap<String, ChartAnalysisPredictionDTO> predictionCache = new ConcurrentHashMap<>();

    // 캐시에 데이터 추가
    public void putPrediction(String symbol, ChartAnalysisPredictionDTO prediction) {
        predictionCache.put(symbol, prediction);
    }

    // 캐시에서 데이터 조회
    public ChartAnalysisPredictionDTO getPrediction(String symbol) {
        return predictionCache.get(symbol);
    }
}
