package com.example.autotradebot.state;

import com.example.autotradebot.dto.analysis.SentimentAnalysisDTO;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SentimentAnalysisCacheManager {

    // ConcurrentHashMap을 사용하여 멀티 스레드 환경에서도 안전하게 상태 관리
    private final ConcurrentHashMap<String, SentimentAnalysisDTO> sentimentCache = new ConcurrentHashMap<>();

    // 캐시에 데이터 추가
    public void putSentiment(String symbol, SentimentAnalysisDTO sentimentAnalysisDTO) {
        sentimentCache.put(symbol, sentimentAnalysisDTO);
    }

    // 캐시에서 데이터 조회
    public SentimentAnalysisDTO getSentiment(String symbol) {
        return sentimentCache.get(symbol);
    }

    // 캐시에서 데이터 삭제
    public void removeSentiment(String symbol) {
        sentimentCache.remove(symbol);
    }
}
