package com.example.autotradebot.state;

import com.example.autotradebot.dto.analysis.ChartAnalysisDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GeminiChartAnalysisCacheManager {

    private final ConcurrentHashMap<String, List<ChartAnalysisDTO>> geminiChartAnalysisCache = new ConcurrentHashMap<>();

    // ✅ 데이터 추가 (200개 초과 시 가장 오래된 데이터 하나 삭제)
    public void putChartAnalysis(String symbol, ChartAnalysisDTO chartAnalysisDTO) {
        geminiChartAnalysisCache.computeIfAbsent(symbol, k -> new ArrayList<>()).add(chartAnalysisDTO);
        removeOldEntries(symbol);
    }

    // ✅ 가장 최신 데이터 반환 (없으면 null)
    public ChartAnalysisDTO getLatestChartAnalysis(String symbol) {
        List<ChartAnalysisDTO> analysisList = geminiChartAnalysisCache.get(symbol);
        return (analysisList != null && !analysisList.isEmpty()) ? analysisList.get(analysisList.size() - 1) : null;
    }

    // ✅ 전체 데이터 반환
    public List<ChartAnalysisDTO> getAllChartAnalysis(String symbol) {
        return geminiChartAnalysisCache.getOrDefault(symbol, new ArrayList<>());
    }

    // ✅ 200개 초과 시 가장 오래된 데이터 **하나만 삭제**
    public void removeOldEntries(String symbol) {
        List<ChartAnalysisDTO> analysisList = geminiChartAnalysisCache.get(symbol);
        if (analysisList != null && analysisList.size() > 200) {
            analysisList.remove(0); // 가장 오래된 데이터 하나 삭제
        }
    }

    // ✅ 특정 심볼 데이터 전체 삭제
    public void removeChartAnalysis(String symbol) {
        geminiChartAnalysisCache.remove(symbol);
    }
}
