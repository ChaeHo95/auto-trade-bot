package com.example.autotradebot.state;

import com.example.autotradebot.dto.analysis.ChartAnalysisDTO;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChartAnalysisCacheManager {

    // ConcurrentHashMap을 사용하여 멀티 스레드 환경에서도 안전하게 상태 관리
    private final ConcurrentHashMap<String, ChartAnalysisDTO> chartAnalysisCache = new ConcurrentHashMap<>();

    // 캐시에 데이터 추가
    public void putChartAnalysis(String symbol, ChartAnalysisDTO chartAnalysisDTO) {
        chartAnalysisCache.put(symbol, chartAnalysisDTO);
    }

    // 캐시에서 데이터 조회
    public ChartAnalysisDTO getChartAnalysis(String symbol) {
        return chartAnalysisCache.get(symbol);
    }

    // 캐시에서 데이터 삭제
    public void removeChartAnalysis(String symbol) {
        chartAnalysisCache.remove(symbol);
    }
}
