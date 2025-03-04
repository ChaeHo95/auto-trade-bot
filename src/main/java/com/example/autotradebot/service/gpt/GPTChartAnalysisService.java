package com.example.autotradebot.service.gpt;

import com.example.autotradebot.dto.analysis.ChartAnalysisPredictionDTO;
import com.example.autotradebot.dto.analysis.MarketAnalysisDTO;
import com.example.autotradebot.service.analysis.ChartAnalysisPredictionService;
import com.example.autotradebot.service.analysis.MarketAnalysisService;
import com.example.autotradebot.state.PredictionCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class GPTChartAnalysisService {

    private Logger logger = LoggerFactory.getLogger(GPTChartAnalysisService.class);

    private final MarketAnalysisService marketAnalysisService;
    private final GPTService gptService;
    private final ChartAnalysisPredictionService predictionService;
    private final PredictionCacheManager predictionCacheManager;

    @Autowired
    public GPTChartAnalysisService(MarketAnalysisService marketAnalysisService,
                                   GPTService gptService,
                                   ChartAnalysisPredictionService predictionService,
                                   PredictionCacheManager predictionCacheManager) {
        this.marketAnalysisService = marketAnalysisService;
        this.gptService = gptService;
        this.predictionService = predictionService;
        this.predictionCacheManager = predictionCacheManager;
    }

    /**
     * ✅ 1분마다 호출되며, 심볼별로 15분이 지난 데이터만 다시 호출
     */
    public void scheduledChartAnalysis(String symbol) {
        // 1️⃣ 최근 차트 데이터 가져오기
        MarketAnalysisDTO marketData = marketAnalysisService.getMarketAnalysis(symbol);

        // 2️⃣ 심볼별로 캐시된 분석 결과 가져오기
        ChartAnalysisPredictionDTO cachedPrediction = predictionCacheManager.getPrediction(symbol);

        if (isVolatilityHigh(marketData, symbol)) {
            // 변동성이 높거나, 캐시와 현재 데이터의 변동률 차이가 크므로 즉시 GPT 호출
            logger.info("변동성이 높거나, 캐시된 데이터와 변동률 차이가 커서 즉시 GPT 호출");
            analyzeChartWithGPT(symbol, marketData);
            return; // 호출 후 바로 종료
        }

        // 3️⃣ 캐시된 데이터가 없거나 15분 이상 지난 경우 호출
        if (cachedPrediction == null || isDataExpired(cachedPrediction)) {
            // 5️⃣ 변동성이 낮으면 15분 후에 다시 호출하도록 예약
            logger.info("변동성이 낮아 15분 후에 호출 예약");
            analyzeChartWithGPT(symbol, marketData);
        } else {
            logger.info("캐시된 데이터가 존재하고 15분 이내이므로 GPT 호출 생략");
        }
    }

    /**
     * ✅ 데이터가 30분 이상 경과했는지 확인
     */
    private boolean isDataExpired(ChartAnalysisPredictionDTO cachedPrediction) {
        return cachedPrediction.getAnalysisTime().isBefore(LocalDateTime.now().minusMinutes(30));
    }

    /**
     * ✅ 변동성 체크 로직 (최근 15분 변동성이 클 경우 true 반환)
     */
    private boolean isVolatilityHigh(MarketAnalysisDTO marketData, String symbol) {
        // 1️⃣ 캐시된 예측 결과 가져오기
        ChartAnalysisPredictionDTO cachedPrediction = predictionCacheManager.getPrediction(symbol);

        // 캐시된 데이터가 없거나 변동성을 계산할 수 없으면 false로 처리
        if (cachedPrediction == null) {
            logger.info("캐시된 데이터가 없어서 변동성 체크를 할 수 없습니다.");
            return false;
        }

        // 2️⃣ 현재 데이터와 캐시된 데이터의 종가 가져오기
        BigDecimal lastClose = marketData.getRecentKlines().get(0).getClosePrice();
        BigDecimal cachedClose = cachedPrediction.getMovingAverage(); // 캐시된 데이터에서 마지막 이동평균 가져오기 (필요에 맞게 수정)

        // 3️⃣ 현재 데이터와 캐시된 데이터의 변동률 계산
        BigDecimal volatility = lastClose.subtract(cachedClose).abs()  // 절대값으로 차이 계산
                .divide(cachedClose, BigDecimal.ROUND_HALF_UP)  // 백분율 계산
                .multiply(BigDecimal.valueOf(3)); // 변동률 %

        // 4️⃣ 변동률이 3% 이상일 경우 true 반환
        return volatility.compareTo(BigDecimal.valueOf(3.0)) > 0; // 3% 이상 변동 시 true 반환
    }

    /**
     * ✅ GPT-4o에게 차트 분석 요청 및 결과 저장
     */
    private void analyzeChartWithGPT(String symbol, MarketAnalysisDTO marketData) {
        // 6️⃣ GPT-4o에게 분석 요청
        ChartAnalysisPredictionDTO chartAnalysisPredictionDTO = gptService.requestChartAnalysis(marketData);

        // 7️⃣ 분석 결과 캐시에 저장
        predictionCacheManager.putPrediction(symbol, chartAnalysisPredictionDTO); // 캐시 저장

        // 8️⃣ DB에도 결과 저장
        predictionService.savePrediction(chartAnalysisPredictionDTO); // DB 저장
    }
}
