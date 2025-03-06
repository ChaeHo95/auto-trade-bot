package com.example.autotradebot.service.chart;

import com.example.autotradebot.dto.analysis.*;
import com.example.autotradebot.dto.binance.BinanceLiquidationOrderDTO;
import com.example.autotradebot.dto.binance.BinancePartialBookDepthDTO;
import com.example.autotradebot.service.analysis.MarketAnalysisService;
import com.example.autotradebot.service.binance.BinanceService;
import com.example.autotradebot.service.gemini.GeminiService;
import com.example.autotradebot.service.gpt.GptService;
import com.example.autotradebot.state.ChatGptChartAnalysisCacheManager;
import com.example.autotradebot.state.ChatGptPredictionCacheManager;
import com.example.autotradebot.state.GeminiChartAnalysisCacheManager;
import com.example.autotradebot.state.GeminiPredictionCacheManager;
import com.google.gson.JsonElement;
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
    private final BinanceService binanceService;

    @Autowired
    public ChartAnalysisService(GeminiChartAnalysisCacheManager geminiChartAnalysisCacheManager,
                                ChatGptChartAnalysisCacheManager chatGptChartAnalysisCacheManager,
                                ChatGptPredictionCacheManager chatGptPredictionCacheManager,
                                GeminiPredictionCacheManager geminiPredictionCacheManager,
                                GeminiService geminiService, MarketAnalysisService marketAnalysisService,
                                PredictionService predictionService, GptService gptService,
                                BinanceService binanceService
    ) {
        this.geminiChartAnalysisCacheManager = geminiChartAnalysisCacheManager;
        this.chatGptChartAnalysisCacheManager = chatGptChartAnalysisCacheManager;
        this.geminiPredictionCacheManager = geminiPredictionCacheManager;
        this.chatGptPredictionCacheManager = chatGptPredictionCacheManager;
        this.geminiService = geminiService;
        this.marketAnalysisService = marketAnalysisService;
        this.predictionService = predictionService;
        this.gptService = gptService;
        this.binanceService = binanceService;
    }

    /**
     * ✅ 1분마다 호출되며, 심볼별로 15분이 지난 데이터만 다시 호출
     */
    public void scheduledChartAnalysis(String symbol, String bot) {
        System.out.println("symbol = " + symbol);
        System.out.println("bot = " + bot);
        if (!bot.equals("GEMINI") && !bot.equals("CHATGPT")) {
            return;
        }

        int limit = bot.equals("CHATGPT") ? 100 : 50;
        // 1️⃣ 최근 차트 데이터 가져오기
        MarketAnalysisDTO marketData = marketAnalysisService.getMarketAnalysis(symbol, bot, limit);

        // 2️⃣ 심볼별로 캐시된 분석 결과 가져오기
        PredictionDTO cachedPrediction = null;

        if (bot.equals("GEMINI")) {
            cachedPrediction = geminiPredictionCacheManager.getLatestPrediction(symbol);
        }
        if (bot.equals("CHATGPT")) {
            cachedPrediction = chatGptPredictionCacheManager.getLatestPrediction(symbol);
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
        String symbol = marketAnalysisDTO.getSymbol();
        String klineSummary = convertKlineToJson(marketAnalysisDTO.getRecentKlines());
        String tradeSummary = convertTradeToJson(marketAnalysisDTO.getRecentTrades());
        String fundingSummary = convertFundingRateToJson(marketAnalysisDTO.getFundingRates());
        String aiAnalysis = convertAiAnalysisToJson(marketAnalysisDTO.getCurrentPosition());
        String partialBookDepth = convertPartialBookDepthToJson(marketAnalysisDTO.getPartialBookDepth());
        String liquidationOrder = convertLiquidationOrderToJson(marketAnalysisDTO.getLiquidationOrders());
        String technicalIndicators = convertTechnicalIndicatorsToJson(marketAnalysisDTO.getMovingAverage(), marketAnalysisDTO.getRsiValue(), marketAnalysisDTO.getMacdValue());

        int limit = bot.equals("CHATGPT") ? 100 : 50;
        String takerBuySellVolume = binanceService.getTakerBuySellVolume(symbol, limit);
        String longShortRatio = binanceService.getLongShortRatio(symbol, limit);
        // 여기서 "5"를 "30"으로 바꾸면 30분으로 설정 가능
        String analysisTimeUnit = bot.equals("CHATGPT") ? "30" : "5";

        // 2️⃣ 시스템 메시지 정의
        String systemMessage = "You are a highly advanced crypto trading assistant specializing in analyzing market trends and price movements. "
                + "Your task is to evaluate the provided market data and generate a recommendation for the best trading position. "
                + "Please ensure to consider all the following factors when making your analysis:\n\n"
                + "- **Recent Price Movements (Candlestick Data):** Analyze the most recent price changes from the 1-minute candlestick data to understand the short-term direction of the market and identify potential price trends.\n"
                + "- **RSI (Relative Strength Index):** Determine if the market is overbought or oversold by calculating the RSI based on the 1-minute candlestick data, helping to gauge whether the market is likely to reverse or continue its trend.\n"
                + "- **MACD (Moving Average Convergence Divergence):** Use the MACD indicator, calculated from 1-minute candlestick data, to evaluate market momentum and potential trend reversals or continuations, based on the relationship between two moving averages.\n"
                + "- **Moving Average (200-period):** Assess the overall market trend using the 200-period moving average based on 1-minute candlestick data, and compare it with short-term movements to identify long-term trends and short-term fluctuations.\n"
                + "- **Funding Rate:** Analyze the funding rate, which reflects market sentiment, liquidity, and biases in market direction, based on 1-minute data intervals to assess market sentiment.\n"
                + "- **Recent Trading Volume:** Review the trading volume associated with the 1-minute candlestick price movements to determine the strength or weakness of the price changes, as higher volume often confirms stronger market moves.\n\n"
                + "Based on your analysis, please provide the following recommendations in your response:\n\n"
                + "- **Trading Position:** Recommend whether to take a LONG, SHORT, EXIT, or WAIT position based on current market conditions and your analysis of all the factors.\n"
                + "- **Confidence Score:** Provide the level of confidence in your recommendation (percentage from 0 to 100), based on the data analysis.\n"
                + "- **Reasoning:** Explain the rationale behind your recommendation, referencing the market factors you considered and how they influence your decision.\n"
                + "- **Stop-Loss Price:** Suggest an appropriate stop-loss price to minimize potential losses based on market volatility.\n"
                + "- **Take-Profit Price:** Suggest a take-profit price based on the current market analysis, considering potential future price movements.\n"
                + "- **Leverage Level:** Recommend a suitable leverage level based on the overall market conditions, risk analysis, and confidence in the recommendation.\n\n"
                + "Please ensure that your recommendation is well-supported by the available data to make the most informed and accurate decision possible.";

        // 3️⃣ 사용자 메시지 정의
        String userMessage = "Analyze the following market data and predict the best trading position:\n\n"
                + "### Trading Symbol: " + symbol + "\n\n"
                + "### Market Data (JSON Format):\n" + klineSummary + "\n\n"
                + "### AI Previous Analysis (from last prediction):\n" + aiAnalysis + "\n\n"
                + "### Trade Summary (from last prediction):\n" + tradeSummary + "\n\n"
                + "### Funding Summary (from last prediction):\n" + fundingSummary + "\n\n"
                + "### Technical Indicators:\n" + technicalIndicators + "\n\n"
                + "### Partial Book Depth:\n" + partialBookDepth + "\n\n"
                + "### Liquidation Orders:\n" + liquidationOrder + "\n\n"
                + "### Taker Buy/Sell Volume:\n" + takerBuySellVolume + "\n\n"
                + "### Long/Short Ratio:\n" + longShortRatio + "\n\n"
                + "### Timeframes for the data:\n"
                + " - **Kline Summary** is based on 1-minute candlestick data.\n"
                + " - **Trade Summary** and **Funding Summary** are based on 1-second intervals.\n\n"
                + "### Important Details:\n"
                + " - **MACD, RSI, and Moving Average** are calculated based on the 1-minute candlestick data.\n\n"
                + "### Analysis Time Unit:\n"
                + " - The analysis is based on the data received at **[current timestamp]**, with 1-minute candlesticks for technical indicators.\n"
                + " - The analysis interval is **every " + analysisTimeUnit + " minutes**.\n\n"
                + "### Your Response Format (JSON):\n"
                + "{\n"
                + "  \"symbol\": \"" + symbol + "\",\n"
                + "  \"position\": \"LONG | SHORT | EXIT | WAIT\",\n"
                + "  \"confidence\": percentage (0-100),\n"
                + "  \"reason\": \"Explain why this position is recommended\",\n"
                + "  \"stopLoss\": \"Recommended stop-loss price\",\n"
                + "  \"takeProfit\": \"Recommended take-profit price\",\n"
                + "  \"leverage\": \"Suggested leverage level (if applicable)\"\n"
                + "}\n";

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


    private String convertKlineToJson(List<MarketAnalysisKlineDTO> marketAnalysisKlineDTOS) {
        if (marketAnalysisKlineDTOS == null || marketAnalysisKlineDTOS.isEmpty()) {
            return "[]"; // 빈 배열 반환
        }

        return marketAnalysisKlineDTOS.stream()
                .map(kline -> {
                    if (kline == null) {
                        return "{}"; // null 요소를 처리
                    }
                    return String.format("{ \"openTime\": %d, \"openPrice\": %.2f, \"highPrice\": %.2f, \"lowPrice\": %.2f, \"closePrice\": %.2f, \"volume\": %.2f, \"closeTime\": %d }",
                            kline.getOpenTime(), kline.getOpenPrice(), kline.getHighPrice(), kline.getLowPrice(), kline.getClosePrice(), kline.getVolume(), kline.getCloseTime());
                })
                .collect(Collectors.joining(",\n"));
    }

    private String convertPartialBookDepthToJson(List<BinancePartialBookDepthDTO> partialBookDepthDTOS) {
        if (partialBookDepthDTOS == null || partialBookDepthDTOS.isEmpty()) {
            return "[]"; // 빈 배열 반환
        }

        return partialBookDepthDTOS.stream()
                .map(depth -> {
                    if (depth == null) {
                        return "{}"; // null 요소를 처리
                    }
                    return String.format("{ \"eventType\": \"%s\", \"eventTime\": %d, \"transactionTime\": %d, \"symbol\": \"%s\"," +
                                    " \"firstUpdateId\": %d, \"finalUpdateId\": %d, \"previousUpdateId\": %d, \"bids\": [%s], \"asks\": [%s] }",
                            depth.getEventType(), depth.getEventTime(), depth.getTransactionTime(), depth.getSymbol(),
                            depth.getFirstUpdateId(), depth.getFinalUpdateId(), depth.getPreviousUpdateId(),
                            convertOrderBookEntryToJson(depth.getBids()), convertOrderBookEntryToJson(depth.getAsks()));
                })
                .collect(Collectors.joining(",\n"));
    }

    private String convertLiquidationOrderToJson(List<BinanceLiquidationOrderDTO> liquidationOrderDTOS) {
        if (liquidationOrderDTOS == null || liquidationOrderDTOS.isEmpty()) {
            return "[]"; // 빈 배열 반환
        }

        return liquidationOrderDTOS.stream()
                .map(order -> {
                    if (order == null || order.getLiquidation() == null) {
                        return "{}"; // null 요소를 처리
                    }
                    return String.format("{ \"eventType\": \"%s\", \"eventTime\": %d, " +
                                    "\"symbol\": \"%s\", \"side\": \"%s\"," +
                                    " \"orderType\": \"%s\", \"timeInForce\": \"%s\", " +
                                    "\"originalQuantity\": %.2f, \"price\": %.2f, \"averagePrice\": %.2f," +
                                    " \"orderStatus\": \"%s\", \"lastFilledQuantity\": %.2f, " +
                                    "\"totalFilledQuantity\": %.2f, \"tradeTime\": %d }",
                            order.getEventType(), order.getEventTime(), order.getLiquidation().getSymbol(), order.getLiquidation().getSide(),
                            order.getLiquidation().getOrderType(), order.getLiquidation().getTimeInForce(), order.getLiquidation().getOriginalQuantity(),
                            order.getLiquidation().getPrice(), order.getLiquidation().getAveragePrice(), order.getLiquidation().getOrderStatus(),
                            order.getLiquidation().getLastFilledQuantity(), order.getLiquidation().getTotalFilledQuantity(), order.getLiquidation().getTradeTime());
                })
                .collect(Collectors.joining(",\n"));
    }

    private String convertOrderBookEntryToJson(List<BinancePartialBookDepthDTO.OrderBookEntry> orderBookEntries) {
        if (orderBookEntries == null || orderBookEntries.isEmpty()) {
            return "[]"; // 빈 배열 반환
        }

        return orderBookEntries.stream()
                .map(entry -> {
                    if (entry == null) {
                        return "{}"; // null 요소를 처리
                    }
                    return String.format("{ \"price\": %.2f, \"quantity\": %.2f }", entry.getPrice(), entry.getQuantity());
                })
                .collect(Collectors.joining(",\n"));
    }

    private String convertTradeToJson(List<MarketAnalysisTradeDTO> marketAnalysisTradeDTOS) {
        if (marketAnalysisTradeDTOS == null || marketAnalysisTradeDTOS.isEmpty()) {
            return "[]"; // 빈 배열 반환
        }

        return marketAnalysisTradeDTOS.stream()
                .map(trade -> {
                    if (trade == null) {
                        return "{}"; // null 요소를 처리
                    }
                    return String.format("{ \"tradeId\": %d, \"price\": %.2f, \"quantity\": %.2f, \"tradeTime\": %d, \"isBuyerMaker\": %b }",
                            trade.getTradeId(), trade.getPrice(), trade.getQuantity(), trade.getTradeTime(), trade.getIsBuyerMaker());
                })
                .collect(Collectors.joining(",\n"));
    }

    private String convertFundingRateToJson(List<MarketAnalysisFundingRateDTO> marketAnalysisFundingRateDTOS) {
        if (marketAnalysisFundingRateDTOS == null || marketAnalysisFundingRateDTOS.isEmpty()) {
            return "[]"; // 빈 배열 반환
        }

        return marketAnalysisFundingRateDTOS.stream()
                .map(funding -> {
                    if (funding == null) {
                        return "{}"; // null 요소를 처리
                    }
                    return String.format("{ \"fundingTime\": %d, \"fundingRate\": %.6f, \"symbol\": \"%s\" }",
                            funding.getFundingTime(), funding.getFundingRate(), funding.getSymbol());
                })
                .collect(Collectors.joining(",\n"));
    }

    private String convertAiAnalysisToJson(PredictionDTO predictionDTO) {
        if (predictionDTO == null) {
            return "{}"; // 예측 데이터가 없으면 빈 객체 반환
        }

        return String.format("{ \"symbol\": \"%s\"," +
                        " \"analysisTime\": \"%s\"," + " \"recommendedPosition\": \"%s\"," + " \"confidenceScore\": %.2f," +
                        " \"movingAverage\": %.2f," + " \"rsiValue\": %.2f," + " \"macdValue\": %.2f," +
                        " \"volatility\": %.2f," + " \"fundingRate\": %.6f," + " \"tradeVolume\": %.2f," + " \"reason\": \"%s\" }",
                safeString(predictionDTO.getSymbol()), safeString(String.valueOf(predictionDTO.getAnalysisTime())), safeString(predictionDTO.getRecommendedPosition()),
                predictionDTO.getConfidenceScore(), predictionDTO.getMovingAverage(), predictionDTO.getRsiValue(),
                predictionDTO.getMacdValue(), predictionDTO.getVolatility(), predictionDTO.getFundingRate(),
                predictionDTO.getTradeVolume(), safeString(predictionDTO.getReason()));
    }

    private String safeString(String value) {
        return value == null || value.trim().isEmpty() ? "N/A" : value;
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
            cachedPrediction = geminiPredictionCacheManager.getLatestPrediction(symbol);
        }
        if (bot.equals("CHATGPT")) {
            cachedPrediction = chatGptPredictionCacheManager.getLatestPrediction(symbol);
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

        // 7️⃣ 분석 결과 캐시에 저장
        if (bot.equals("GEMINI")) {
            geminiPredictionCacheManager.putPrediction(symbol, predictionDTO); // 캐시 저장
        }
        if (bot.equals("CHATGPT")) {
            chatGptPredictionCacheManager.putPrediction(symbol, predictionDTO); // 캐시 저장
        }


        if (predictionDTO == null) {
            return;
        }

        predictionDTO.setBotType(bot);
        // 8️⃣ DB에도 결과 저장
        predictionService.savePrediction(predictionDTO); // DB 저장
    }

    private String getSafeJsonString(JsonObject json, String key, String defaultValue) {
        if (json.has(key)) {
            JsonElement element = json.get(key);

            // 값이 null 이거나 비어있는지 체크
            if (!element.isJsonNull() && !element.getAsString().trim().isEmpty()) {
                return element.getAsString();
            }
        }
        return defaultValue; // 기본값 반환
    }
}
