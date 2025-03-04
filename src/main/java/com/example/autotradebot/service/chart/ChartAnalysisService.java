package com.example.autotradebot.service.chart;

import com.example.autotradebot.dto.analysis.*;
import com.example.autotradebot.service.analysis.MarketAnalysisService;
import com.example.autotradebot.service.gemini.GeminiService;
import com.example.autotradebot.service.gpt.GptService;
import com.example.autotradebot.state.ChatGptChartAnalysisCacheManager;
import com.example.autotradebot.state.ChatGptPredictionCacheManager;
import com.example.autotradebot.state.GeminiChartAnalysisCacheManager;
import com.example.autotradebot.state.GeminiPredictionCacheManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChartAnalysisService {

    private Logger logger = LoggerFactory.getLogger(ChartAnalysisService.class);

    private final GeminiChartAnalysisCacheManager geminiChartAnalysisCacheManager;
    private final ChatGptChartAnalysisCacheManager chatGptChartAnalysisCacheManager;
    private final GeminiService geminiService;
    private final GptService gptService;
    private final MarketAnalysisService marketAnalysisService;
    private final ChatGptPredictionCacheManager chatGptPredictionCacheManager;
    private final GeminiPredictionCacheManager geminiPredictionCacheManager;
    private final PredictionService predictionService;

    @Autowired
    public ChartAnalysisService(GeminiChartAnalysisCacheManager geminiChartAnalysisCacheManager,
                                ChatGptChartAnalysisCacheManager chatGptChartAnalysisCacheManager,
                                ChatGptPredictionCacheManager chatGptPredictionCacheManager,
                                GeminiPredictionCacheManager geminiPredictionCacheManager,
                                GeminiService geminiService, MarketAnalysisService marketAnalysisService,
                                PredictionService predictionService, GptService gptService) {
        this.geminiChartAnalysisCacheManager = geminiChartAnalysisCacheManager;
        this.chatGptChartAnalysisCacheManager = chatGptChartAnalysisCacheManager;
        this.geminiPredictionCacheManager = geminiPredictionCacheManager;
        this.chatGptPredictionCacheManager = chatGptPredictionCacheManager;
        this.geminiService = geminiService;
        this.marketAnalysisService = marketAnalysisService;
        this.predictionService = predictionService;
        this.gptService = gptService;
    }

    /**
     * ✅ 1분마다 호출되며, 심볼별로 15분이 지난 데이터만 다시 호출
     */
    public void scheduledChartAnalysis(String symbol, String bot) {
        if (!bot.equals("GEMINI") && !bot.equals("CHATGPT")) {
            return;
        }


        // 1️⃣ 최근 차트 데이터 가져오기
        MarketAnalysisDTO marketData = marketAnalysisService.getMarketAnalysis(symbol, bot);

        // 2️⃣ 심볼별로 캐시된 분석 결과 가져오기
        PredictionDTO cachedPrediction = null;

        if (bot.equals("GEMINI")) {
            cachedPrediction = geminiPredictionCacheManager.getLatestPrediction(symbol); // 캐시 저장
        }
        if (bot.equals("CHATGPT")) {
            cachedPrediction = chatGptPredictionCacheManager.getLatestPrediction(symbol); // 캐시 저장
        }

        if (isVolatilityHigh(marketData, bot, symbol)) {
            // 변동성이 높거나, 캐시와 현재 데이터의 변동률 차이가 크므로 즉시 GPT 호출

            if (bot.equals("GEMINI")) {
                logger.info("변동률 차이가 커서 즉시 GEMINI 호출");
            }
            if (bot.equals("CHATGPT")) {
                logger.info("변동률 차이가 커서 즉시 GPT 호출");
            }
            analyzeChart(symbol, marketData, bot);
            return; // 호출 후 바로 종료
        }

        // 3️⃣ 캐시된 데이터가 없거나 15분 이상 지난 경우 호출
        if (cachedPrediction == null || isDataExpired(cachedPrediction, bot)) {
            // 5️⃣ 변동성이 낮으면 15분 후에 다시 호출하도록 예약
            if (bot.equals("GEMINI")) {
                logger.info("변동성이 낮아 5분 후에 호출 예약");
            }
            if (bot.equals("CHATGPT")) {
                logger.info("변동성이 낮아 30분 후에 호출 예약");
            }
            analyzeChart(symbol, marketData, bot);
        } else {
            if (bot.equals("GEMINI")) {
                logger.info("캐시된 데이터가 존재하고 5분 전 이므로 GEMINI 호출 생략");
            }
            if (bot.equals("CHATGPT")) {
                logger.info("캐시된 데이터가 존재하고 30분 전 이므로 CHATGPT 호출 생략");
            }

        }
    }


    /**
     * ✅차트 분석 요청을 보냄
     */
    private PredictionDTO chartAnalysis(MarketAnalysisDTO marketAnalysisDTO, String bot) {

        // 1️⃣ 각 데이터를 JSON 형식으로 변환
        String klineSummary = convertKlineToJson(marketAnalysisDTO.getRecentKlines());
        String tradeSummary = convertTradeToJson(marketAnalysisDTO.getRecentTrades());
        String fundingSummary = convertFundingRateToJson(marketAnalysisDTO.getFundingRates());
        String aiAnalysis = convertAiAnalysisToJson(marketAnalysisDTO.getCurrentPosition());
        String technicalIndicators = convertTechnicalIndicatorsToJson(marketAnalysisDTO.getMovingAverage(), marketAnalysisDTO.getRsiValue(), marketAnalysisDTO.getMacdValue());

        String systemMessage = "You are a highly advanced crypto trading assistant specializing in analyzing market trends and price movements. " + "Your task is to evaluate the provided market data and generate a recommendation for the best trading position. " + "Please ensure to consider all the following factors when making your analysis:\n\n" + "- **Recent Price Movements (Candlestick Data):** Analyze recent price changes for understanding price direction.\n" + "- **RSI (Relative Strength Index):** Determine overbought or oversold conditions to gauge market strength.\n" + "- **MACD (Moving Average Convergence Divergence):** Identify momentum and potential reversals or continuations.\n" + "- **Moving Average (200-period):** Assess the overall market trend and compare it with short-term movements.\n" + "- **Funding Rate:** Understand market sentiment and liquidity to evaluate potential biases in market direction.\n" + "- **Recent Trading Volume:** Review recent trade volumes to assess the strength of price movements.\n\n" + "Based on your analysis, please provide the following recommendations in your response:\n\n" + "- **Trading Position:** Recommend whether to take a LONG, SHORT, EXIT, or WAIT position based on current market conditions.\n" + "- **Confidence Score:** Provide the level of confidence in your recommendation (percentage from 0 to 100).\n" + "- **Reasoning:** Explain the rationale behind your recommendation, referencing the market factors you considered.\n" + "- **Stop-Loss Price:** Suggest an appropriate stop-loss price to minimize potential losses.\n" + "- **Take-Profit Price:** Suggest a take-profit price based on the current market analysis.\n" + "- **Leverage Level:** Recommend a suitable leverage level based on the overall market conditions.\n\n" + "Please ensure that your recommendation is well-supported by the available data to make the most informed and accurate decision possible.";


        // 3️⃣ 사용자 데이터 입력 (전체 데이터 JSON 포함)
        String userMessage = String.format("Analyze the following market data and predict the best trading position:\n\n" + "### Trading Symbol: %s\n\n" + "### Market Data (JSON Format):\n%s\n\n" + "### AI Previous Analysis (from last prediction):\n%s\n\n" + "### Technical Indicators:\n%s\n\n" + "### Your Response Format (JSON):\n" + "{\n" + "  \"symbol\": \"%s\",\n" + "  \"position\": \"LONG | SHORT | EXIT | WAIT\",\n" + "  \"confidence\": percentage (0-100),\n" + "  \"reason\": \"Explain why this position is recommended\",\n" + "  \"stopLoss\": \"Recommended stop-loss price\",\n" + "  \"takeProfit\": \"Recommended take-profit price\",\n" + "  \"leverage\": \"Suggested leverage level (if applicable)\"\n" + "}\n", marketAnalysisDTO.getSymbol(), klineSummary, tradeSummary, fundingSummary, aiAnalysis, technicalIndicators, marketAnalysisDTO.getSymbol());


        // 3️⃣ OpenAI API 호출
        String response = null;

        if (bot.equals("GEMINI")) {
            response = geminiService.callGeminiAiApi(systemMessage, userMessage);
        } else if (bot.equals("CHATGPT")) {
            response = gptService.callOpenAiApi(systemMessage, userMessage);
        }

        if (response == null) {
            return null;
        }

        JsonReader reader = new JsonReader(new StringReader(response));
        reader.setLenient(true);  // lenient 파싱 허용
        JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

        return mapToChartAnalysisPredictionDTO(bot, jsonObject, marketAnalysisDTO);
    }


    // ✅ 최근 200개 캔들 데이터 JSON 변환
    private String convertKlineToJson(List<MarketAnalysisKlineDTO> marketAnalysisKlineDTOS) {
        return marketAnalysisKlineDTOS.stream().map(kline -> String.format("{ \"openTime\": %d, \"openPrice\": %.2f, \"highPrice\": %.2f, \"lowPrice\": %.2f, \"closePrice\": %.2f, \"volume\": %.2f, \"closeTime\": %d }", kline.getOpenTime(), kline.getOpenPrice(), kline.getHighPrice(), kline.getLowPrice(), kline.getClosePrice(), kline.getVolume(), kline.getCloseTime())).collect(Collectors.joining(",\n"));
    }

    // ✅ 최근 100개 체결 거래 데이터 JSON 변환
    private String convertTradeToJson(List<MarketAnalysisTradeDTO> marketAnalysisTradeDTOS) {
        return marketAnalysisTradeDTOS.stream().map(trade -> String.format("{ \"tradeId\": %d, \"price\": %.2f, \"quantity\": %.2f, \"tradeTime\": %d, \"isBuyerMaker\": %b }", trade.getTradeId(), trade.getPrice(), trade.getQuantity(), trade.getTradeTime(), trade.getIsBuyerMaker())).collect(Collectors.joining(",\n"));
    }

    // ✅ 최근 20개 펀딩 비율 데이터 JSON 변환
    private String convertFundingRateToJson(List<MarketAnalysisFundingRateDTO> marketAnalysisFundingRateDTOS) {
        return marketAnalysisFundingRateDTOS.stream().map(funding -> String.format("{ \"fundingTime\": %d, \"fundingRate\": %.6f, \"symbol\": \"%s\" }", funding.getFundingTime(), funding.getFundingRate(), funding.getSymbol())).collect(Collectors.joining(",\n"));
    }

    // ✅ AI 분석 데이터 JSON 변환
    private String convertAiAnalysisToJson(PredictionDTO predictionDTO) {
        return predictionDTO != null ? String.format("{ \"symbol\": \"%s\", \"analysisTime\": \"%s\", \"recommendedPosition\": \"%s\", \"confidenceScore\": %.2f, \"movingAverage\": %.2f, \"rsiValue\": %.2f, \"macdValue\": %.2f, \"volatility\": %.2f, \"fundingRate\": %.6f, \"tradeVolume\": %.2f, \"reason\": \"%s\" }", predictionDTO.getSymbol(), predictionDTO.getAnalysisTime(), predictionDTO.getRecommendedPosition(), predictionDTO.getConfidenceScore(), predictionDTO.getMovingAverage(), predictionDTO.getRsiValue(), predictionDTO.getMacdValue(), predictionDTO.getVolatility(), predictionDTO.getFundingRate(), predictionDTO.getTradeVolume(), predictionDTO.getReason()) : "{}";
    }

    // ✅ 기술 지표들 (이동평균선, RSI, MACD 값) JSON 변환
    private String convertTechnicalIndicatorsToJson(BigDecimal movingAverage, BigDecimal rsiValue, BigDecimal macdValue) {
        return String.format("{ \"movingAverage\": %.2f, \"rsiValue\": %.2f, \"macdValue\": %.2f }", movingAverage, rsiValue, macdValue);
    }

    private PredictionDTO mapToChartAnalysisPredictionDTO(String bot, JsonObject jsonResponse, MarketAnalysisDTO marketAnalysisDTO) {

        // 각 필드에 대해 null 체크 및 "N/A" 값 처리
        BigDecimal movingAverage = (marketAnalysisDTO.getMovingAverage() != null) ? marketAnalysisDTO.getMovingAverage() : BigDecimal.ZERO;
        BigDecimal rsiValue = (marketAnalysisDTO.getRsiValue() != null) ? marketAnalysisDTO.getRsiValue() : BigDecimal.ZERO;
        BigDecimal macdValue = (marketAnalysisDTO.getMacdValue() != null) ? marketAnalysisDTO.getMacdValue() : BigDecimal.ZERO;
        BigDecimal volatility = (marketAnalysisDTO.getMacdValue() != null) ? marketAnalysisDTO.getMacdValue() : BigDecimal.ZERO;
        BigDecimal fundingRate = marketAnalysisDTO.getFundingRates().stream().map(MarketAnalysisFundingRateDTO::getFundingRate) // 각 펀딩 비율을 가져옵니다.
                .reduce(BigDecimal.ZERO, BigDecimal::add); // 모든 펀딩 비율을 더합니다. 초기값은 0
        BigDecimal tradeVolume = marketAnalysisDTO.getRecentTrades().stream().map(MarketAnalysisTradeDTO::getQuantity) // 각 거래의 거래량을 가져옵니다.
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 포지션과 신뢰도
        String position = getSafeJsonString(jsonResponse, "position", "WAIT");
        String reason = getSafeJsonString(jsonResponse, "reason", "N/A");
        String stopLoss = getSafeJsonString(jsonResponse, "stopLoss", "N/A");
        String takeProfit = getSafeJsonString(jsonResponse, "takeProfit", "N/A");
        String leverage = getSafeJsonString(jsonResponse, "leverage", "N/A");
        BigDecimal confidence = jsonResponse.has("confidence") ? jsonResponse.get("confidence").getAsBigDecimal() : BigDecimal.ZERO;

        // 값이 모두 null일 경우 예외 처리나 경고 추가 (필요에 따라)
        if (rsiValue.compareTo(BigDecimal.ZERO) == 0) {
            logger.warn("Warning: RSI value is not provided, defaulting to 0.");
        }

        ChartAnalysisDTO chartAnalysisDTO = ChartAnalysisDTO.builder()
                .symbol(marketAnalysisDTO.getSymbol())
                .position(position)
                .confidence(confidence)
                .reason(reason)
                .stopLoss(stopLoss)
                .takeProfit(takeProfit)
                .leverage(leverage)
                .build();


        if (bot.equals("GEMINI")) {
            geminiChartAnalysisCacheManager.putChartAnalysis(marketAnalysisDTO.getSymbol(), chartAnalysisDTO);
        }
        if (bot.equals("CHATGPT")) {
            chatGptChartAnalysisCacheManager.putChartAnalysis(marketAnalysisDTO.getSymbol(), chartAnalysisDTO);
        }


        return PredictionDTO.builder()
                .botType(bot)
                .symbol(marketAnalysisDTO.getSymbol())
                .analysisTime(LocalDateTime.now()) // 현재 분석 시각
                .recommendedPosition(position) // 포지션 (LONG, SHORT, EXIT, WAIT)
                .confidenceScore(confidence) // 예측 신뢰도
                .movingAverage(movingAverage) // 이동평균선
                .rsiValue(rsiValue) // RSI 값
                .macdValue(macdValue) // MACD 값
                .volatility(volatility) // 변동성
                .fundingRate(fundingRate) // 펀딩 비율
                .tradeVolume(tradeVolume) // 거래량
                .reason(reason).build();
    }


    /**
     * ✅ 데이터가 30분 이상 경과했는지 확인
     */
    private boolean isDataExpired(PredictionDTO cachedPrediction, String bot) {
        if (bot.equals("GEMINI")) {
            return cachedPrediction.getAnalysisTime().isBefore(LocalDateTime.now().minusMinutes(5));
        } else if (bot.equals("CHATGPT")) {
            return cachedPrediction.getAnalysisTime().isBefore(LocalDateTime.now().minusMinutes(30));
        } else {
            return false;
        }
    }

    /**
     * ✅ 변동성 체크 로직 (최근 15분 변동성이 클 경우 true 반환)
     */
    private boolean isVolatilityHigh(MarketAnalysisDTO marketData, String bot, String symbol) {
        // 1️⃣ 캐시된 예측 결과 가져오기
        PredictionDTO cachedPrediction = null;
        if (bot.equals("GEMINI")) {
            cachedPrediction = geminiPredictionCacheManager.getLatestPrediction(symbol); // 캐시 저장
        }
        if (bot.equals("CHATGPT")) {
            cachedPrediction = chatGptPredictionCacheManager.getLatestPrediction(symbol); // 캐시 저장
        }
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
     * ✅ 차트 분석 요청 및 결과 저장
     */
    private void analyzeChart(String symbol, MarketAnalysisDTO marketData, String bot) {

        PredictionDTO predictionDTO = chartAnalysis(marketData, bot);

        if (bot.equals("GEMINI")) {
            geminiPredictionCacheManager.putPrediction(symbol, predictionDTO); // 캐시 저장
        }
        if (bot.equals("CHATGPT")) {
            chatGptPredictionCacheManager.putPrediction(symbol, predictionDTO); // 캐시 저장
        }
        // 7️⃣ 분석 결과 캐시에 저장


        // 8️⃣ DB에도 결과 저장
        predictionService.savePrediction(predictionDTO); // DB 저장
    }

    private String getSafeJsonString(JsonObject json, String key, String defaultValue) {
        return (json.has(key) && !json.get(key).isJsonNull()) ? json.get(key).getAsString() : defaultValue;
    }
}
