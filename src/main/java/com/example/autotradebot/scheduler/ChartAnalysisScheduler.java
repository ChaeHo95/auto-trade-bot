package com.example.autotradebot.scheduler;

import com.example.autotradebot.service.chart.ChartAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Locale;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "enable.ai.scheduling", havingValue = "true", matchIfMissing = false)
public class ChartAnalysisScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ChartAnalysisScheduler.class);


    private final ChartAnalysisService chartAnalysisService;

    @Value("${symbols}")
    private List<String> symbols;

    @Autowired
    public ChartAnalysisScheduler(ChartAnalysisService chartAnalysisService) {
        this.chartAnalysisService = chartAnalysisService;
    }

    /**
     * ✅ 1분마다 호출되며, 심볼별로 15분이 지난 데이터만 다시 호출
     */
    @Scheduled(fixedRate = 60000, initialDelay = 600000)  // 1분마다 실행 (60,000ms = 1분)
    public void scheduledChartAnalysis() {
        logger.info("차트 분석 스케줄 시작 {}", symbols);

        List<String> markets = symbols.stream()
                .map(v -> v.toLowerCase(Locale.ROOT))
                .toList(); // 필요한 심볼 추가 가능

        // 각 심볼에 대해 주기적으로 분석을 수행
        for (String market : markets) {
            logger.info("처리 중인 시장: {}", market);

            // 1️⃣ 최근 차트 데이터 가져오기
            // 여기서 차트 데이터를 가져오는 로직을 로그로 찍어줍니다
            logger.info("차트 데이터 가져오기 시작: {}", market);

            // 2️⃣ GPT 차트 분석 서비스 호출
            logger.info("GPT 차트 분석 서비스 호출 시작: {}", market);
            chartAnalysisService.scheduledChartAnalysis(market.toUpperCase(), "GEMINI");
            logger.info("GPT 차트 분석 서비스 호출 완료: {}", market);

            // 3️⃣ ChatGPT 차트 분석 서비스 호출
            logger.info("ChatGPT 차트 분석 서비스 호출 시작: {}", market);
            chartAnalysisService.scheduledChartAnalysis(market.toUpperCase(), "CHATGPT");
            logger.info("ChatGPT 차트 분석 서비스 호출 완료: {}", market);
        }

        logger.info("차트 분석 스케줄 종료");
    }
}
