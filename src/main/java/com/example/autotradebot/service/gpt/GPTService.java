package com.example.autotradebot.service.gpt;

import com.example.autotradebot.dto.analysis.*;
import com.example.autotradebot.dto.bot.OpenAiResponseDTO;
import com.example.autotradebot.state.ChartAnalysisCacheManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GPTService {

    private Logger logger = LoggerFactory.getLogger(GPTService.class);
    private final WebClient webClient;

    private final ChartAnalysisCacheManager chartAnalysisCacheManager;

    @Autowired
    public GPTService(ChartAnalysisCacheManager chartAnalysisCacheManager, @Qualifier("openAiApiClient") WebClient openAiApiClient) {
        this.webClient = openAiApiClient;
        this.chartAnalysisCacheManager = chartAnalysisCacheManager;
    }

    /**
     * ✅ GPT-4o에게 차트 분석 요청을 보냄
     */
    public ChartAnalysisPredictionDTO requestChartAnalysis(MarketAnalysisDTO marketAnalysisDTO) {

        // 1️⃣ 각 데이터를 JSON 형식으로 변환
        String klineSummary = convertKlineToJson(marketAnalysisDTO.getRecentKlines());
        String tradeSummary = convertTradeToJson(marketAnalysisDTO.getRecentTrades());
        String fundingSummary = convertFundingRateToJson(marketAnalysisDTO.getFundingRates());
        String aiAnalysis = convertAiAnalysisToJson(marketAnalysisDTO.getCurrentPosition());
        String technicalIndicators = convertTechnicalIndicatorsToJson(
                marketAnalysisDTO.getMovingAverage(),
                marketAnalysisDTO.getRsiValue(),
                marketAnalysisDTO.getMacdValue()
        );

        String systemMessage =
                "You are a highly advanced crypto trading assistant specializing in analyzing market trends and price movements. "
                        + "Your task is to evaluate the provided market data and generate a recommendation for the best trading position. "
                        + "Please ensure to consider all the following factors when making your analysis:\n\n"
                        + "- **Recent Price Movements (Candlestick Data):** Analyze recent price changes for understanding price direction.\n"
                        + "- **RSI (Relative Strength Index):** Determine overbought or oversold conditions to gauge market strength.\n"
                        + "- **MACD (Moving Average Convergence Divergence):** Identify momentum and potential reversals or continuations.\n"
                        + "- **Moving Average (200-period):** Assess the overall market trend and compare it with short-term movements.\n"
                        + "- **Funding Rate:** Understand market sentiment and liquidity to evaluate potential biases in market direction.\n"
                        + "- **Recent Trading Volume:** Review recent trade volumes to assess the strength of price movements.\n\n"
                        + "Based on your analysis, please provide the following recommendations in your response:\n\n"
                        + "- **Trading Position:** Recommend whether to take a LONG, SHORT, EXIT, or WAIT position based on current market conditions.\n"
                        + "- **Confidence Score:** Provide the level of confidence in your recommendation (percentage from 0 to 100).\n"
                        + "- **Reasoning:** Explain the rationale behind your recommendation, referencing the market factors you considered.\n"
                        + "- **Stop-Loss Price:** Suggest an appropriate stop-loss price to minimize potential losses.\n"
                        + "- **Take-Profit Price:** Suggest a take-profit price based on the current market analysis.\n"
                        + "- **Leverage Level:** Recommend a suitable leverage level based on the overall market conditions.\n\n"
                        + "Please ensure that your recommendation is well-supported by the available data to make the most informed and accurate decision possible.";


        // 3️⃣ 사용자 데이터 입력 (전체 데이터 JSON 포함)
        String userMessage = String.format(
                "Analyze the following market data and predict the best trading position:\n\n"
                        + "### Trading Symbol: %s\n\n"
                        + "### Market Data (JSON Format):\n%s\n\n"
                        + "### AI Previous Analysis (from last prediction):\n%s\n\n"
                        + "### Technical Indicators:\n%s\n\n"
                        + "### Your Response Format (JSON):\n"
                        + "{\n"
                        + "  \"symbol\": \"%s\",\n"
                        + "  \"position\": \"LONG | SHORT | EXIT | WAIT\",\n"
                        + "  \"confidence\": percentage (0-100),\n"
                        + "  \"reason\": \"Explain why this position is recommended\",\n"
                        + "  \"stopLoss\": \"Recommended stop-loss price\",\n"
                        + "  \"takeProfit\": \"Recommended take-profit price\",\n"
                        + "  \"leverage\": \"Suggested leverage level (if applicable)\"\n"
                        + "}\n",
                marketAnalysisDTO.getSymbol(), klineSummary, tradeSummary, fundingSummary, aiAnalysis,
                technicalIndicators, marketAnalysisDTO.getSymbol()
        );


        // 3️⃣ OpenAI API 호출
        ChartAnalysisPredictionDTO responseDto = callOpenAiApi(systemMessage, userMessage, marketAnalysisDTO);

        // 4️⃣ 응답이 존재하면 첫 번째 결과 가져오기
        if (responseDto != null) {
            return responseDto;
        }

        return null;
    }


    /**
     * WebClient를 이용하여 OpenAI API 호출
     */
    private ChartAnalysisPredictionDTO callOpenAiApi(String systemMessage, String userMessage, MarketAnalysisDTO marketAnalysisDTO) {
        try {
            // Create request body
            JsonObject requestBodyJson = new JsonObject();
            requestBodyJson.addProperty("model", "gpt-4o-mini");

            JsonArray messages = new JsonArray();
            JsonObject systemMessageJson = new JsonObject();
            systemMessageJson.addProperty("role", "system");
            systemMessageJson.addProperty("content", systemMessage);
            messages.add(systemMessageJson);

            JsonObject userMessageJson = new JsonObject();
            userMessageJson.addProperty("role", "user");
            userMessageJson.addProperty("content", userMessage);
            messages.add(userMessageJson);

            requestBodyJson.add("messages", messages);
            requestBodyJson.addProperty("temperature", 0.7);

            String requestBody = requestBodyJson.toString();

            // Make API call to OpenAI
            OpenAiResponseDTO response = webClient.post()
                    .uri("/chat/completions")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(OpenAiResponseDTO.class)
                    .block();

            if (response != null) {
                logger.info("OpenAI response: {}", response);

                // Parse the message content from the response
                String content = null;
                if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                    // Assuming we want the content of the first choice
                    content = response.getChoices().get(0).getMessage().getContent();
                }

                if (content != null) {
                    logger.info("Received content: {}", content);

                    String cleanedResponseText = content.replace("```json", "").replace("```", "").trim();
                    // JsonObject로 응답을 파싱
                    JsonReader reader = new JsonReader(new StringReader(cleanedResponseText));

                    reader.setLenient(true);  // lenient 파싱 허용
                    JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

                    return mapToChartAnalysisPredictionDTO(jsonObject, marketAnalysisDTO);
                } else {
                    logger.error("No valid content found in the OpenAI response.");
                }
            } else {
                logger.error("No response from OpenAI API.");
                return null;
            }

            return null;
        } catch (Exception e) {
            logger.error("Error calling OpenAI API", e);
            return null;
        }
    }


    // ✅ 최근 200개 캔들 데이터 JSON 변환
    private String convertKlineToJson(List<MarketAnalysisKlineDTO> marketAnalysisKlineDTOS) {
        return marketAnalysisKlineDTOS.stream()
                .map(kline -> String.format(
                        "{ \"openTime\": %d, \"openPrice\": %.2f, \"highPrice\": %.2f, \"lowPrice\": %.2f, \"closePrice\": %.2f, \"volume\": %.2f, \"closeTime\": %d }",
                        kline.getOpenTime(), kline.getOpenPrice(), kline.getHighPrice(),
                        kline.getLowPrice(), kline.getClosePrice(), kline.getVolume(), kline.getCloseTime()
                ))
                .collect(Collectors.joining(",\n"));
    }

    // ✅ 최근 100개 체결 거래 데이터 JSON 변환
    private String convertTradeToJson(List<MarketAnalysisTradeDTO> marketAnalysisTradeDTOS) {
        return marketAnalysisTradeDTOS.stream()
                .map(trade -> String.format(
                        "{ \"tradeId\": %d, \"price\": %.2f, \"quantity\": %.2f, \"tradeTime\": %d, \"isBuyerMaker\": %b }",
                        trade.getTradeId(), trade.getPrice(), trade.getQuantity(), trade.getTradeTime(),
                        trade.getIsBuyerMaker()
                ))
                .collect(Collectors.joining(",\n"));
    }

    // ✅ 최근 20개 펀딩 비율 데이터 JSON 변환
    private String convertFundingRateToJson(List<MarketAnalysisFundingRateDTO> marketAnalysisFundingRateDTOS) {
        return marketAnalysisFundingRateDTOS.stream()
                .map(funding -> String.format(
                        "{ \"fundingTime\": %d, \"fundingRate\": %.6f, \"symbol\": \"%s\" }",
                        funding.getFundingTime(), funding.getFundingRate(), funding.getSymbol()
                ))
                .collect(Collectors.joining(",\n"));
    }

    // ✅ AI 분석 데이터 JSON 변환
    private String convertAiAnalysisToJson(ChartAnalysisPredictionDTO chartAnalysisPredictionDTO) {
        return chartAnalysisPredictionDTO != null ? String.format(
                "{ \"symbol\": \"%s\", \"analysisTime\": \"%s\", \"recommendedPosition\": \"%s\", \"confidenceScore\": %.2f, \"movingAverage\": %.2f, \"rsiValue\": %.2f, \"macdValue\": %.2f, \"volatility\": %.2f, \"fundingRate\": %.6f, \"tradeVolume\": %.2f, \"reason\": \"%s\", \"createdAt\": \"%s\" }",
                chartAnalysisPredictionDTO.getSymbol(),
                chartAnalysisPredictionDTO.getAnalysisTime(),
                chartAnalysisPredictionDTO.getRecommendedPosition(),
                chartAnalysisPredictionDTO.getConfidenceScore(),
                chartAnalysisPredictionDTO.getMovingAverage(),
                chartAnalysisPredictionDTO.getRsiValue(),
                chartAnalysisPredictionDTO.getMacdValue(),
                chartAnalysisPredictionDTO.getVolatility(),
                chartAnalysisPredictionDTO.getFundingRate(),
                chartAnalysisPredictionDTO.getTradeVolume(),
                chartAnalysisPredictionDTO.getReason(),
                chartAnalysisPredictionDTO.getCreatedAt()
        ) : "{}";
    }

    // ✅ 기술 지표들 (이동평균선, RSI, MACD 값) JSON 변환
    private String convertTechnicalIndicatorsToJson(BigDecimal movingAverage, BigDecimal rsiValue, BigDecimal macdValue) {
        return String.format(
                "{ \"movingAverage\": %.2f, \"rsiValue\": %.2f, \"macdValue\": %.2f }",
                movingAverage, rsiValue, macdValue
        );
    }

    private ChartAnalysisPredictionDTO mapToChartAnalysisPredictionDTO(JsonObject jsonResponse, MarketAnalysisDTO marketAnalysisDTO) {

        // 각 필드에 대해 null 체크 및 "N/A" 값 처리
        BigDecimal movingAverage = (marketAnalysisDTO.getMovingAverage() != null) ? marketAnalysisDTO.getMovingAverage() : BigDecimal.ZERO;
        BigDecimal rsiValue = (marketAnalysisDTO.getRsiValue() != null) ? marketAnalysisDTO.getRsiValue() : BigDecimal.ZERO;
        BigDecimal macdValue = (marketAnalysisDTO.getMacdValue() != null) ? marketAnalysisDTO.getMacdValue() : BigDecimal.ZERO;
        BigDecimal volatility = (marketAnalysisDTO.getMacdValue() != null) ? marketAnalysisDTO.getMacdValue() : BigDecimal.ZERO;
        BigDecimal fundingRate = marketAnalysisDTO.getFundingRates().stream()
                .map(MarketAnalysisFundingRateDTO::getFundingRate) // 각 펀딩 비율을 가져옵니다.
                .reduce(BigDecimal.ZERO, BigDecimal::add); // 모든 펀딩 비율을 더합니다. 초기값은 0
        BigDecimal tradeVolume = marketAnalysisDTO.getRecentTrades().stream()
                .map(MarketAnalysisTradeDTO::getQuantity) // 각 거래의 거래량을 가져옵니다.
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 포지션과 신뢰도
        String position = jsonResponse.has("position") ? jsonResponse.get("position").getAsString() : "WAIT";
        String reason = jsonResponse.has("reason") ? jsonResponse.get("reason").getAsString() : "";
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
                .stopLoss(jsonResponse.has("stopLoss") ? jsonResponse.get("stopLoss").getAsString() : "N/A")
                .takeProfit(jsonResponse.has("takeProfit") ? jsonResponse.get("takeProfit").getAsString() : "N/A")
                .leverage(jsonResponse.has("getAsBigDecimal") ? jsonResponse.get("getAsBigDecimal").getAsString() : "N/A")
                .build();

        // 캐시 저장
        chartAnalysisCacheManager.putChartAnalysis(marketAnalysisDTO.getSymbol(), chartAnalysisDTO);

        return ChartAnalysisPredictionDTO.builder()
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
                .createdAt(LocalDateTime.now()) // 데이터 생성 시각
                .reason(reason)
                .build();
    }
}
