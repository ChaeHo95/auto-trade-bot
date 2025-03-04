package com.example.autotradebot.state;

import com.example.autotradebot.dto.analysis.ChartAnalysisDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatGptChartAnalysisCacheManager {

    private final ConcurrentHashMap<String, List<ChartAnalysisDTO>> chatGptChartAnalysisCache = new ConcurrentHashMap<>();

    public void putChartAnalysis(String symbol, ChartAnalysisDTO chartAnalysisDTO) {
        chatGptChartAnalysisCache.computeIfAbsent(symbol, k -> new ArrayList<>()).add(chartAnalysisDTO);
        removeOldEntries(symbol);
    }

    public ChartAnalysisDTO getLatestChartAnalysis(String symbol) {
        List<ChartAnalysisDTO> analysisList = chatGptChartAnalysisCache.get(symbol);
        return (analysisList != null && !analysisList.isEmpty()) ? analysisList.get(analysisList.size() - 1) : null;
    }

    public List<ChartAnalysisDTO> getAllChartAnalysis(String symbol) {
        return chatGptChartAnalysisCache.getOrDefault(symbol, new ArrayList<>());
    }

    public void removeOldEntries(String symbol) {
        List<ChartAnalysisDTO> analysisList = chatGptChartAnalysisCache.get(symbol);
        if (analysisList != null && analysisList.size() > 200) {
            analysisList.remove(0); // 가장 오래된 데이터 하나 삭제
        }
    }

    public void removeChartAnalysis(String symbol) {
        chatGptChartAnalysisCache.remove(symbol);
    }
}
