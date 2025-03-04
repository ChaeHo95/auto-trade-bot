package com.example.autotradebot.scheduler.gpt;

import com.example.autotradebot.service.analysis.MarketAnalysisService;
import com.example.autotradebot.service.gpt.GPTChartAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class GPTChartAnalysisScheduler {

    private final GPTChartAnalysisService gptChartAnalysisService;
    private final MarketAnalysisService marketAnalysisService;

    @Value("symbols")
    private List<String> symbols;

    @Autowired
    public GPTChartAnalysisScheduler(GPTChartAnalysisService gptChartAnalysisService,
                                     MarketAnalysisService marketAnalysisService) {
        this.gptChartAnalysisService = gptChartAnalysisService;
        this.marketAnalysisService = marketAnalysisService;
    }

    /**
     * ✅ 1분마다 호출되며, 심볼별로 15분이 지난 데이터만 다시 호출
     */
//    @Scheduled(fixedRate = 60000) // 1분마다 실행 (60,000ms = 1분)
    public void scheduledChartAnalysis() {
        List<String> markets = symbols.stream()
                .map(v -> v.toLowerCase(Locale.ROOT) + "usdt")
                .toList(); // 필요한 심볼 추가 가능

        // 각 심볼에 대해 주기적으로 분석을 수행
        for (String market : markets) {
            // 1️⃣ 최근 차트 데이터 가져오기
            // 2️⃣ GPT 차트 분석 서비스 호출
            gptChartAnalysisService.scheduledChartAnalysis(market.toUpperCase());
        }
    }
}
