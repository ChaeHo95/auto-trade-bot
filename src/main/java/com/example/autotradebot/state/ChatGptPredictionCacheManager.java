package com.example.autotradebot.state;

import com.example.autotradebot.dto.analysis.PredictionDTO;
import com.example.autotradebot.mapper.analysis.MarketAnalysisMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatGptPredictionCacheManager {

    @Value("${symbols}")
    private List<String> symbols;

    private final ConcurrentHashMap<String, List<PredictionDTO>> chatGptPredictionCache = new ConcurrentHashMap<>();

    private MarketAnalysisMapper marketAnalysisMapper;

    @Autowired
    private ChatGptPredictionCacheManager(MarketAnalysisMapper marketAnalysisMapper) {
        this.marketAnalysisMapper = marketAnalysisMapper;
    }

    @PostConstruct
    public void init() {
        for (String symbol : symbols) {
            List<PredictionDTO> predictionDTOList = marketAnalysisMapper.getChartAnalysisLimit("CHATGPT", symbol, 200);
            predictionDTOList.forEach(predictionDTO -> {
                putPrediction(symbol, predictionDTO);
            });
        }
    }

    // ✅ 데이터 추가 (200개 초과 시 가장 오래된 데이터 하나 삭제)
    public void putPrediction(String symbol, PredictionDTO predictionDTO) {
        chatGptPredictionCache.computeIfAbsent(symbol, k -> new ArrayList<>()).add(predictionDTO);
        removeOldEntries(symbol);
    }

    // ✅ 가장 최신 데이터 반환 (없으면 null)
    public PredictionDTO getLatestPrediction(String symbol) {
        List<PredictionDTO> predictionList = chatGptPredictionCache.get(symbol);
        return (predictionList != null && !predictionList.isEmpty()) ? predictionList.get(predictionList.size() - 1) : null;
    }

    // ✅ 전체 데이터 반환
    public List<PredictionDTO> getAllPredictions(String symbol) {
        return chatGptPredictionCache.getOrDefault(symbol, new ArrayList<>());
    }

    // ✅ 200개 초과 시 가장 오래된 데이터 **하나만 삭제**
    public void removeOldEntries(String symbol) {
        List<PredictionDTO> predictionList = chatGptPredictionCache.get(symbol);
        if (predictionList != null && predictionList.size() > 200) {
            predictionList.remove(0); // 가장 오래된 데이터 하나 삭제
        }
    }

    // ✅ 특정 심볼 데이터 전체 삭제
    public void removePrediction(String symbol) {
        chatGptPredictionCache.remove(symbol);
    }
}
