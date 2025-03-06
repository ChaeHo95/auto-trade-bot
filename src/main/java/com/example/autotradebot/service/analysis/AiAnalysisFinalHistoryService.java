package com.example.autotradebot.service.analysis;


import com.example.autotradebot.dto.analysis.*;
import com.example.autotradebot.dto.binance.BinanceLiquidationOrderDTO;
import com.example.autotradebot.dto.binance.BinanceOrderBookEntryDTO;
import com.example.autotradebot.dto.binance.BinancePartialBookDepthDTO;
import com.example.autotradebot.mapper.analysis.AiAnalysisFinalHistoryMapper;
import com.example.autotradebot.mapper.analysis.MarketAnalysisMapper;
import com.example.autotradebot.service.binance.BinanceService;
import com.example.autotradebot.service.gemini.GeminiService;
import com.example.autotradebot.state.*;
import com.example.autotradebot.util.JsonUtils;
import com.example.autotradebot.util.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiAnalysisFinalHistoryService {

    private final AiAnalysisFinalHistoryMapper aiAnalysisFinalHistoryMapper;

    private ChatGptChartAnalysisCacheManager chatGptChartAnalysisCacheManager;
    private ChatGptPredictionCacheManager chatGptPredictionCacheManager;
    private GeminiChartAnalysisCacheManager geminiChartAnalysisCacheManager;
    private GeminiPredictionCacheManager geminiPredictionCacheManager;
    private final BinanceService binanceService;

    private GeminiService geminiService;
    private MarketAnalysisMapper marketAnalysisMapper;
    private JsonUtils jsonUtils;

    private PositionCacheManager positionCacheManager;

    @Autowired
    public AiAnalysisFinalHistoryService(AiAnalysisFinalHistoryMapper aiAnalysisFinalHistoryMapper,
                                         ChatGptChartAnalysisCacheManager chatGptChartAnalysisCacheManager,
                                         ChatGptPredictionCacheManager chatGptPredictionCacheManager,
                                         GeminiChartAnalysisCacheManager geminiChartAnalysisCacheManager,
                                         GeminiPredictionCacheManager geminiPredictionCacheManager,
                                         GeminiService geminiService, MarketAnalysisMapper marketAnalysisMapper,
                                         JsonUtils jsonUtils, BinanceService binanceService,
                                         PositionCacheManager positionCacheManager
    ) {
        this.aiAnalysisFinalHistoryMapper = aiAnalysisFinalHistoryMapper;
        this.chatGptChartAnalysisCacheManager = chatGptChartAnalysisCacheManager;
        this.chatGptPredictionCacheManager = chatGptPredictionCacheManager;
        this.geminiPredictionCacheManager = geminiPredictionCacheManager;
        this.geminiChartAnalysisCacheManager = geminiChartAnalysisCacheManager;
        this.geminiService = geminiService;
        this.marketAnalysisMapper = marketAnalysisMapper;
        this.jsonUtils = jsonUtils;
        this.binanceService = binanceService;
        this.positionCacheManager = positionCacheManager;
    }

    // ✅ AI 분석
    public void processAiAnalysisFinal(String symbol) {

        List<ChartAnalysisDTO> chatGptChartAnalysis = chatGptChartAnalysisCacheManager.getAllChartAnalysis(symbol);
        List<PredictionDTO> chatGptPredictions = chatGptPredictionCacheManager.getAllPredictions(symbol);
        List<ChartAnalysisDTO> geminiChartAnalysis = geminiChartAnalysisCacheManager.getAllChartAnalysis(symbol);
        List<PredictionDTO> geminiPredictions = geminiPredictionCacheManager.getAllPredictions(symbol);


        List<MarketAnalysisKlineDTO> recentKlines = marketAnalysisMapper.getRecentKlines(symbol, 500)
                .stream()
                .map(kline -> MarketAnalysisKlineDTO.builder()
                        .openTime(kline.getOpenTime())
                        .openPrice(kline.getOpenPrice())
                        .highPrice(kline.getHighPrice())
                        .lowPrice(kline.getLowPrice())
                        .closePrice(kline.getClosePrice())
                        .volume(kline.getVolume())
                        .closeTime(kline.getCloseTime())
                        .build())
                .collect(Collectors.toList());

        List<MarketAnalysisTradeDTO> recentTrades = marketAnalysisMapper.getRecentTrades(symbol, 500)
                .stream()
                .map(trade -> MarketAnalysisTradeDTO.builder()
                        .price(trade.getPrice())
                        .quantity(trade.getQuantity())
                        .tradeTime(trade.getTradeTime())
                        .isBuyerMaker(trade.getBuyerMaker())
                        .build())
                .collect(Collectors.toList());


        List<MarketAnalysisFundingRateDTO> fundingRates = marketAnalysisMapper.getFundingRates(symbol, 500)
                .stream()
                .map(fundingRate -> MarketAnalysisFundingRateDTO.builder()
                        .fundingTime(fundingRate.getFundingTime())
                        .fundingRate(fundingRate.getFundingRate())
                        .symbol(fundingRate.getSymbol())
                        .mark_price(fundingRate.getMarkPrice())
                        .build())
                .collect(Collectors.toList());


        List<BinanceLiquidationOrderDTO> liquidationOrders = marketAnalysisMapper.getLiquidationOrders(symbol, 500);

        List<BinancePartialBookDepthDTO> partialBookDepth = marketAnalysisMapper.getPartialBookDepth(symbol, 500);

        for (BinancePartialBookDepthDTO depth : partialBookDepth) {
            List<BinanceOrderBookEntryDTO> orderBookEntries = marketAnalysisMapper.getOrderBookEntriesBySymbolAndEventTime(symbol, depth.getEventTime());
            List<BinancePartialBookDepthDTO.OrderBookEntry> bids = new ArrayList();
            List<BinancePartialBookDepthDTO.OrderBookEntry> asks = new ArrayList();
            for (BinanceOrderBookEntryDTO entry : orderBookEntries) {
                BinancePartialBookDepthDTO.OrderBookEntry orderBookEntry = new BinancePartialBookDepthDTO.OrderBookEntry(entry.getPrice(), entry.getQuantity());
                if ("BID".equals(entry.getOrderType())) {

                    bids.add(orderBookEntry);
                } else {
                    asks.add(orderBookEntry);
                }
            }
            depth.setBids(bids);
            depth.setAsks(asks);
        }

        BigDecimal movingAverage = marketAnalysisMapper.getMovingAverage(symbol, 500);
        BigDecimal rsiValue = marketAnalysisMapper.getRSIValue(symbol);
        BigDecimal macdValue = marketAnalysisMapper.getMACDValue(symbol);

        String analysisTimeUnit = "5 minutes";  // 데이터 분석이 5분마다 진행된다고 명시
        String partialBookDepthToJson = convertPartialBookDepthToJson(partialBookDepth);
        String convertLiquidationOrderToJson = convertLiquidationOrderToJson(liquidationOrders);
        String takerBuySellVolume = binanceService.getTakerBuySellVolume(symbol, 500);
        String longShortRatio = binanceService.getLongShortRatio(symbol, 500);
        Position cacheManagerPosition = positionCacheManager.getPosition(symbol);


        String systemMessage =
                "You are a highly advanced crypto trading assistant specializing in analyzing market trends and price movements. " +
                        "Your task is to evaluate the provided market data and generate a recommendation for the best trading position. " +
                        "Please ensure to consider all the following factors when making your analysis:\n\n" +
                        "- **Recent Price Movements (Candlestick Data):** Analyze recent price changes for understanding price direction.\n" +
                        "- **RSI (Relative Strength Index):** Determine overbought or oversold conditions to gauge market strength.\n" +
                        "- **MACD (Moving Average Convergence Divergence):** Identify momentum and potential reversals or continuations.\n" +
                        "- **Moving Average (200-period):** Assess the overall market trend and compare it with short-term movements.\n" +
                        "- **Funding Rate:** Understand market sentiment and liquidity to evaluate potential biases in market direction.\n" +
                        "- **Recent Trading Volume:** Review recent trade volumes to assess the strength of price movements.\n\n" +
                        "Additionally, consider the following:\n" +
                        "- **Previous Chart Analysis (GPT Analysis):** Include analysis results from ChatGPT, including trading positions, confidence scores, and reasoning from previous analyses.\n" +
                        "- **Previous Chart Analysis (Gemini Analysis):** Include analysis results from Gemini, such as recommended positions, confidence scores, and reasoning from past predictions.\n" +
                        "Based on your analysis, please provide the following recommendations in your response:\n\n" +
                        "- **Trading Position:** Recommend whether to take a LONG, SHORT, EXIT, or WAIT position based on current market conditions.\n" +
                        "- **Confidence Score:** Provide the level of confidence in your recommendation (percentage from 0 to 100).\n" +
                        "- **Reasoning:** Explain the rationale behind your recommendation, referencing the market factors you considered.\n" +
                        "- **Stop-Loss Price:** Suggest an appropriate stop-loss price to minimize potential losses.\n" +
                        "- **Take-Profit Price:** Suggest a take-profit price based on the current market analysis.\n" +
                        "- **Leverage Level:** Recommend a suitable leverage level based on the overall market conditions.\n\n" +
                        "Ensure that your recommendation is well-supported by the available data, previous analysis results, and all relevant market factors. " +
                        "Your analysis should integrate recent price movements, technical indicators, funding rates, trade volumes, and previous predictions to provide a well-rounded and actionable trading recommendation.";


        String userMessage =
                "Analyze the following market data and predict the best trading position:\n\n" +
                        "### Trading Symbol: " + symbol + "\n\n" +
                        "### Current Position: " + cacheManagerPosition.toJson() + "\n\n" +
                        "### Market Data (JSON Format):\n" +
                        "{\n" +
                        "  \"recentKlines\": " + convertToJson(recentKlines) + ",\n" + // 1분 간격의 Kline 데이터
                        "  \"recentTrades\": " + convertToJson(recentTrades) + ",\n" + // 1초 간격의 Trade 데이터
                        "  \"fundingRates\": " + convertToJson(fundingRates) + ",\n" +  // 1초 간격의 Funding Rate 데이터
                        "  \"takerBuySellVolume\": " + takerBuySellVolume + ",\n" +  // Taker Buy/Sell Volume 데이터
                        "  \"longShortRatio\": " + longShortRatio + ",\n" +  // Long/Short Ratio 데이터
                        "  \"liquidationOrders\": " + convertLiquidationOrderToJson + ",\n" +  // Liquidation Orders 데이터
                        "  \"partialBookDepth\": " + partialBookDepthToJson + "\n" +  // Partial Book Depth 데이터
                        "}\n\n" +
                        "### Previous Analysis (ChatGPT - Last Prediction):\n" +
                        convertToJson(chatGptChartAnalysis) + "\n\n" + // ChatGPT 이전 분석 결과
                        "### Previous Analysis (Gemini - Last Prediction):\n" +
                        convertToJson(geminiChartAnalysis) + "\n\n" + // Gemini 이전 분석 결과
                        "### Predictions (ChatGPT):\n" +
                        convertToJson(chatGptPredictions) + "\n\n" + // ChatGPT 이전 예측 결과
                        "### Predictions (Gemini):\n" +
                        convertToJson(geminiPredictions) + "\n\n" + // Gemini 이전 예측 결과
                        "### Technical Indicators (from 1-minute Candlestick data):\n" +
                        "{\n" +
                        "  \"movingAverage\": " + String.format("%.2f", movingAverage) + ",\n" + // 1분봉 이동평균
                        "  \"rsiValue\": " + String.format("%.2f", rsiValue) + ",\n" + // 1분봉 RSI
                        "  \"macdValue\": " + String.format("%.2f", macdValue) + "\n" + // 1분봉 MACD
                        "}\n\n" +
                        "### Timeframes for the data:\n" +
                        " - **Kline Summary** is based on 1-minute candlestick data.\n" +
                        " - **Trade Summary** and **Funding Summary** are based on 1-second intervals.\n\n" +
                        "### Analysis Time Unit:\n" +
                        " - The analysis is based on the data received at **[current timestamp]**, with 1-minute candlesticks for technical indicators.\n" +
                        " - The analysis interval is **every " + analysisTimeUnit + " minutes**.\n\n" +
                        "### Your Response Format (JSON):\n" +
                        "{\n" +
                        "  \"symbol\": \"" + symbol + "\",\n" +
                        "  \"analysisTime\": \"" + LocalDateTime.now() + "\",\n" +
                        "  \"recommendedPosition\": \"" + "LONG" + "\",\n" + // 예시 recommendedPosition
                        "  \"executedPosition\": \"" + "WAIT" + "\",\n" + // 예시 executedPosition
                        "  \"profitLoss\": \"" + "0.15" + "\",\n" + // 예시 profitLoss
                        "  \"confidenceScore\": \"" + "85" + "\",\n" + // 예시 confidenceScore
                        "  \"reason\": \"" + "The market is currently in a sideways trend, and further confirmation is needed before making a decision." + "\"\n" + // 예시 reason
                        "}\n";


        String response = geminiService.callGeminiAiApi(systemMessage, userMessage);

        JsonReader reader = new JsonReader(new StringReader(response));
        reader.setLenient(true);  // lenient 파싱 허용
        JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

        AiAnalysisFinalHistoryDTO aiAnalysisFinalHistoryDTO = new AiAnalysisFinalHistoryDTO();

        aiAnalysisFinalHistoryDTO.setId(jsonUtils.getBigInteger(jsonObject, "id"));
        aiAnalysisFinalHistoryDTO.setSymbol(jsonUtils.getString(jsonObject, "symbol"));
        aiAnalysisFinalHistoryDTO.setAnalysisTime(jsonUtils.getLocalDateTime(jsonObject, "analysisTime"));
        aiAnalysisFinalHistoryDTO.setRecommendedPosition(jsonUtils.getString(jsonObject, "recommendedPosition"));
        aiAnalysisFinalHistoryDTO.setExecutedPosition(jsonUtils.getString(jsonObject, "executedPosition"));
        aiAnalysisFinalHistoryDTO.setProfitLoss(jsonUtils.getBigDecimal(jsonObject, "profitLoss"));
        aiAnalysisFinalHistoryDTO.setConfidenceScore(jsonUtils.getBigDecimal(jsonObject, "confidenceScore"));
        aiAnalysisFinalHistoryDTO.setReason(jsonUtils.getString(jsonObject, "reason"));

        saveAiAnalysisFinalHistory(aiAnalysisFinalHistoryDTO);
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

    private String convertToJson(List<?> dataList) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())  // ✅ LocalDateTime 변환 지원 추가
                .create();
        return gson.toJson(dataList);
    }

    // ✅ AI 분석 결과 저장
    public void saveAiAnalysisFinalHistory(AiAnalysisFinalHistoryDTO aiAnalysisFinalHistory) {
        aiAnalysisFinalHistoryMapper.insertAiAnalysisFinalHistory(aiAnalysisFinalHistory);
    }

    // ✅ 특정 심볼의 최신 AI 분석 결과 조회
    public AiAnalysisFinalHistoryDTO getLatestAnalysis(String symbol) {
        return aiAnalysisFinalHistoryMapper.getLatestAiAnalysisFinalHistory(symbol);
    }

    // ✅ 특정 심볼의 전체 AI 분석 결과 조회
    public List<AiAnalysisFinalHistoryDTO> getAllAnalysis(String symbol) {
        return aiAnalysisFinalHistoryMapper.getAllAiAnalysisFinalHistory(symbol);
    }

    // ✅ 특정 심볼의 AI 분석 데이터 삭제
    public void deleteAnalysis(String symbol) {
        aiAnalysisFinalHistoryMapper.deleteAiAnalysisFinalHistory(symbol);
    }
}
