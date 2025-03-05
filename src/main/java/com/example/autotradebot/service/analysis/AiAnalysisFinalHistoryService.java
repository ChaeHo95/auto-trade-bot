package com.example.autotradebot.service.analysis;


import com.example.autotradebot.dto.analysis.*;
import com.example.autotradebot.mapper.analysis.AiAnalysisFinalHistoryMapper;
import com.example.autotradebot.mapper.analysis.MarketAnalysisMapper;
import com.example.autotradebot.service.gemini.GeminiService;
import com.example.autotradebot.state.ChatGptChartAnalysisCacheManager;
import com.example.autotradebot.state.ChatGptPredictionCacheManager;
import com.example.autotradebot.state.GeminiChartAnalysisCacheManager;
import com.example.autotradebot.state.GeminiPredictionCacheManager;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiAnalysisFinalHistoryService {

    private final AiAnalysisFinalHistoryMapper aiAnalysisFinalHistoryMapper;

    private ChatGptChartAnalysisCacheManager chatGptChartAnalysisCacheManager;
    private ChatGptPredictionCacheManager chatGptPredictionCacheManager;
    private GeminiChartAnalysisCacheManager geminiChartAnalysisCacheManager;
    private GeminiPredictionCacheManager geminiPredictionCacheManager;

    private GeminiService geminiService;
    private MarketAnalysisMapper marketAnalysisMapper;
    private JsonUtils jsonUtils;

    @Autowired
    public AiAnalysisFinalHistoryService(AiAnalysisFinalHistoryMapper aiAnalysisFinalHistoryMapper,
                                         ChatGptChartAnalysisCacheManager chatGptChartAnalysisCacheManager,
                                         ChatGptPredictionCacheManager chatGptPredictionCacheManager,
                                         GeminiChartAnalysisCacheManager geminiChartAnalysisCacheManager,
                                         GeminiPredictionCacheManager geminiPredictionCacheManager,
                                         GeminiService geminiService, MarketAnalysisMapper marketAnalysisMapper,
                                         JsonUtils jsonUtils
    ) {
        this.aiAnalysisFinalHistoryMapper = aiAnalysisFinalHistoryMapper;
        this.chatGptChartAnalysisCacheManager = chatGptChartAnalysisCacheManager;
        this.chatGptPredictionCacheManager = chatGptPredictionCacheManager;
        this.geminiPredictionCacheManager = geminiPredictionCacheManager;
        this.geminiChartAnalysisCacheManager = geminiChartAnalysisCacheManager;
        this.geminiService = geminiService;
        this.marketAnalysisMapper = marketAnalysisMapper;
        this.jsonUtils = jsonUtils;
    }

    // ✅ AI 분석
    public void processAiAnalysisFinal(String symbol) {
        List<ChartAnalysisDTO> chatGptChartAnalysis = chatGptChartAnalysisCacheManager.getAllChartAnalysis(symbol);
        List<PredictionDTO> chatGptPredictions = chatGptPredictionCacheManager.getAllPredictions(symbol);
        List<ChartAnalysisDTO> geminiChartAnalysis = geminiChartAnalysisCacheManager.getAllChartAnalysis(symbol);
        List<PredictionDTO> geminiPredictions = geminiPredictionCacheManager.getAllPredictions(symbol);


        List<MarketAnalysisKlineDTO> recentKlines = marketAnalysisMapper.getRecentKlines(symbol, 1000)
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

        List<MarketAnalysisTradeDTO> recentTrades = marketAnalysisMapper.getRecentTrades(symbol, 1000)
                .stream()
                .map(trade -> MarketAnalysisTradeDTO.builder()
                        .price(trade.getPrice())
                        .quantity(trade.getQuantity())
                        .tradeTime(trade.getTradeTime())
                        .isBuyerMaker(trade.getBuyerMaker())
                        .build())
                .collect(Collectors.toList());


        List<MarketAnalysisFundingRateDTO> fundingRates = marketAnalysisMapper.getFundingRates(symbol, 1000)
                .stream()
                .map(fundingRate -> MarketAnalysisFundingRateDTO.builder()
                        .fundingTime(fundingRate.getFundingTime())
                        .fundingRate(fundingRate.getFundingRate())
                        .symbol(fundingRate.getSymbol())
                        .mark_price(fundingRate.getMarkPrice())
                        .build())
                .collect(Collectors.toList());

        BigDecimal movingAverage = marketAnalysisMapper.getMovingAverage(symbol, 1000);
        BigDecimal rsiValue = marketAnalysisMapper.getRSIValue(symbol);
        BigDecimal macdValue = marketAnalysisMapper.getMACDValue(symbol);

        String analysisTimeUnit = "5 minutes";  // 데이터 분석이 5분마다 진행된다고 명시

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
                        "Ensure that your recommendation is well-supported by the available data and previous analysis results.";


        String userMessage = String.format(
                "Analyze the following market data and predict the best trading position:\n\n" +
                        "### Trading Symbol: %s\n\n" +
                        "### Market Data (JSON Format):\n" +
                        "{\n" +
                        "  \"recentKlines\": %s,\n" + // 1초 간격의 Kline 데이터
                        "  \"recentTrades\": %s,\n" + // 1초 간격의 Trade 데이터
                        "  \"fundingRates\": %s\n" +  // 1초 간격의 Funding Rate 데이터
                        "}\n\n" +
                        "### Previous Analysis (ChatGPT - Last Prediction):\n" +
                        "%s\n\n" + // ChatGPT 이전 분석 결과
                        "### Previous Analysis (Gemini - Last Prediction):\n" +
                        "%s\n\n" + // Gemini 이전 분석 결과
                        "### Predictions (ChatGPT):\n" +
                        "%s\n\n" + // ChatGPT 이전 예측 결과
                        "### Predictions (Gemini):\n" +
                        "%s\n\n" + // Gemini 이전 예측 결과
                        "### Technical Indicators (from 1-minute Candlestick data):\n" +
                        "{\n" +
                        "  \"movingAverage\": %.2f,\n" + // 1분봉 이동평균
                        "  \"rsiValue\": %.2f,\n" + // 1분봉 RSI
                        "  \"macdValue\": %.2f\n" + // 1분봉 MACD
                        "}\n\n" +
                        "### Timeframes for the data:\n" +
                        " - **Kline Summary** is based on 1-minute candlestick data.\n" +
                        " - **Trade Summary** and **Funding Summary** are based on 1-second intervals.\n\n" +
                        "### Analysis Time Unit:\n" +
                        " - The analysis is based on the data received at **[current timestamp]**, with 1-minute candlesticks for technical indicators.\n" +
                        " - The analysis interval is **every %s minutes**.\n\n" +
                        "### Your Response Format (JSON):\n" +
                        "{\n" +
                        "  \"symbol\": \"%s\",\n" +
                        "  \"analysisTime\": \"%s\",\n" +
                        "  \"recommendedPosition\": \"%s\",\n" +
                        "  \"executedPosition\": \"%s\",\n" +
                        "  \"profitLoss\": \"%s\",\n" +
                        "  \"confidenceScore\": \"%s\",\n" +
                        "  \"reason\": \"%s\"\n" +  // 분석 이유 추가
                        "}\n",
                symbol,
                convertToJson(recentKlines),
                convertToJson(recentTrades),
                convertToJson(fundingRates),
                convertToJson(chatGptChartAnalysis),
                convertToJson(geminiChartAnalysis),
                convertToJson(chatGptPredictions),
                convertToJson(geminiPredictions),
                movingAverage, rsiValue, macdValue,
                analysisTimeUnit, // 분석 시간 단위
                symbol,
                LocalDateTime.now(), // 분석 시간
                "LONG", // 예시 recommendedPosition
                "WAIT", // 예시 executedPosition
                "0.15", // 예시 profitLoss
                "85", // 예시 confidenceScore
                "The market is currently in a sideways trend, and further confirmation is needed before making a decision." // 예시 reason
        );

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
