package com.example.autotradebot.state;

import com.example.autotradebot.dto.analysis.PredictionDTO;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PredictionCacheManager {

    // ConcurrentHashMap을 사용하여 멀티 스레드 환경에서도 안전하게 상태 관리
    private final ConcurrentHashMap<String, HashMap<String, PredictionDTO>> predictionCache = new ConcurrentHashMap<>();

    // 캐시에 데이터 추가
    public void putPrediction(String bot, String symbol, PredictionDTO prediction) {
        if (predictionCache.containsKey(bot)) {
            predictionCache.get(bot).put(symbol, prediction);
        } else {
            HashMap<String, PredictionDTO> map = new HashMap<>();
            map.put(symbol, prediction);
            predictionCache.put(symbol, map);
        }

    }

    // 캐시에서 데이터 조회
    public PredictionDTO getPrediction(String bot, String symbol) {
        return predictionCache.get(bot).get(symbol);
    }
}
