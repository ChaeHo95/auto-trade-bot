package com.example.autotradebot.service.gemini;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GeminiService {
    private final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private final WebClient webClient;

    // 요청 설정값을 필드 변수로 선언
    private final double temperature = 0.2;
    private final int topK = 40;
    private final double topP = 0.95;
    private final int maxOutputTokens = 2048;
    private final String responseMimeType = "application/json";

    public GeminiService(WebClient.Builder webClientBuilder, @Qualifier("geminiApiUrl") String geminiApiUrl) {
        this.webClient = webClientBuilder
                .baseUrl(geminiApiUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * ✅ Gemini AI를 호출하여 시장 분석을 수행하는 메서드 (비동기 방식)
     */
    public Mono<String> requestGemini(String symbol) {
        JsonObject requestBody = createRequestBody(symbol);
        return getGeminiResponse(requestBody);
    }

    /**
     * ✅ Gemini API에 프롬프트 요청 후 응답 반환 (비동기 방식)
     */
    private Mono<String> getGeminiResponse(JsonObject requestBody) {
        logger.info("📤 Sending request to Gemini API: {}", requestBody);

        return webClient.post()
                .bodyValue(requestBody.toString())
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> logger.info("📥 Received response from Gemini API: {}", response))
                .flatMap(this::parseResponse)
                .onErrorResume(error -> {
                    logger.error("❌ Gemini API 호출 중 오류 발생: {}", error.getMessage());
                    return Mono.just("❌ Gemini API 오류");
                });
    }

    /**
     * ✅ JSON 응답을 파싱하여 결과를 반환하는 메서드 (비동기 방식)
     */
    private Mono<String> parseResponse(String responseBody) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            // ✅ "candidates" 배열 체크
            if (!jsonResponse.has("candidates") || jsonResponse.getAsJsonArray("candidates").size() == 0) {
                logger.error("❌ Gemini API 응답 오류: candidates 필드 없음\n응답 데이터: {}", responseBody);
                return Mono.just("❌ 응답 오류: candidates 필드 없음");
            }

            JsonObject firstCandidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();

            // ✅ "content" 체크
            if (!firstCandidate.has("content")) {
                logger.error("❌ Gemini API 응답 오류: content 필드 없음\n응답 데이터: {}", responseBody);
                return Mono.just("❌ 응답 오류: content 필드 없음");
            }

            JsonObject content = firstCandidate.getAsJsonObject("content");

            // ✅ "parts" 배열 체크
            if (!content.has("parts") || content.getAsJsonArray("parts").size() == 0) {
                logger.error("❌ Gemini API 응답 오류: parts 배열이 비어 있음\n응답 데이터: {}", responseBody);
                return Mono.just("❌ 응답 오류: parts 배열이 비어 있음");
            }

            // ✅ "text" 가져오기 (이중 JSON 문자열 포함)
            String rawJsonString = content.getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();

            // ✅ **이중 파싱 (문자열을 JSON 객체로 변환)**
            JsonObject parsedResponse = JsonParser.parseString(rawJsonString).getAsJsonObject();

            // ✅ 필요한 데이터 추출
            String symbol = parsedResponse.has("symbol") ? parsedResponse.get("symbol").getAsString() : "N/A";
            String timestamp = parsedResponse.has("timestamp") ? parsedResponse.get("timestamp").getAsString() : "N/A";
            String overallSentiment = parsedResponse.has("market_context") && parsedResponse.getAsJsonObject("market_context").has("overall_market")
                    ? parsedResponse.getAsJsonObject("market_context").get("overall_market").getAsString() : "N/A";
            String recommendation = parsedResponse.has("trading_recommendation") && parsedResponse.getAsJsonObject("trading_recommendation").has("direction")
                    ? parsedResponse.getAsJsonObject("trading_recommendation").get("direction").getAsString() : "N/A";
            String comment = parsedResponse.has("trading_recommendation") && parsedResponse.getAsJsonObject("trading_recommendation").has("risk_level")
                    ? parsedResponse.getAsJsonObject("trading_recommendation").get("risk_level").getAsString() : "N/A";

            // ✅ 로깅
            logger.info("✅ Gemini 분석 결과 - Symbol: {}, Timestamp: {}, Market Sentiment: {}, Recommendation: {}, Risk Level: {}",
                    symbol, timestamp, overallSentiment, recommendation, comment);

            return Mono.just(parsedResponse.toString());

        } catch (Exception e) {
            logger.error("❌ 응답 파싱 중 오류 발생: {}\n에러 메시지: {}", responseBody, e.getMessage(), e);
            return Mono.error(new RuntimeException("❌ 응답 파싱 오류: " + e.getMessage()));
        }
    }

    /**
     * ✅ JSON 기반 요청 본문 생성 (프롬프트 포함)
     */
    private JsonObject createRequestBody(String symbol) {
        JsonObject requestBody = new JsonObject();

        // "contents" 배열 추가
        JsonArray contentsArray = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");

        JsonArray partsArray = new JsonArray();
        JsonObject textObject = new JsonObject();
        textObject.addProperty("text", generateMarketAnalysisPrompt(symbol));
        partsArray.add(textObject);
        userMessage.add("parts", partsArray);
        contentsArray.add(userMessage);
        requestBody.add("contents", contentsArray);

        // "generationConfig" 추가 (JSON 응답 강제)
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", temperature);
        generationConfig.addProperty("topK", topK);
        generationConfig.addProperty("topP", topP);
        generationConfig.addProperty("maxOutputTokens", maxOutputTokens);
        generationConfig.addProperty("responseMimeType", responseMimeType);
        requestBody.add("generationConfig", generationConfig);

        return requestBody;
    }

    /**
     * ✅ 시장 분석을 위한 프롬프트 생성
     */
    private String generateMarketAnalysisPrompt(String symbol) {
        return "You are a highly specialized AI trained specifically for **Futures Trading Market Sentiment Analysis** in cryptocurrency markets. " +
                "Your primary goal is to analyze **real-time futures market data** for the trading symbol **" + symbol + "** and generate a structured **JSON-based trading recommendation** tailored for **leveraged futures trading**." +
                "\n\n" +
                "### **Analysis Criteria (Futures-Specific Metrics)**" +
                "\n- Extract real-time **sentiment scores** from futures market data sources (news, social media, order book, and derivatives analytics)." +
                "\n- Retrieve and categorize **news articles** as positive, negative, or neutral based on their impact on " + symbol + " futures trading." +
                "\n- Analyze **tweet volume and sentiment** related to " + symbol + " to identify leveraged market sentiment shifts." +
                "\n- Evaluate **social media discussions (Reddit, crypto forums, etc.)** to determine leveraged trader sentiment." +
                "\n- Consider recent **macro-economic events (FOMC meetings, CPI data, interest rate hikes, regulatory announcements, etc.)** and their impact on **futures trading activity**." +
                "\n- Assess the overall **cryptocurrency futures market context (bull, bear, sideways) using Bitcoin, Ethereum, and derivatives indicators.**" +
                "\n\n" +
                "### **Futures Trading-Specific Metrics:**" +
                "\n- **Funding Rate:** Retrieve the latest funding rate for " + symbol + " futures and determine its impact on trader positioning." +
                "\n- **Open Interest:** Extract real-time **futures open interest** data to assess whether leveraged market participation is increasing or decreasing." +
                "\n- **Liquidation Data:** Identify **large liquidation events** (longs vs. shorts) in the futures market to detect price reversal zones." +
                "\n- **Leverage Ratio:** Analyze the **estimated leverage ratio** to determine **trader risk exposure and potential liquidation risks.**" +
                "\n- **Market Trend:** Determine whether " + symbol + " futures are in a **bullish, bearish, or sideways trend** based on derivatives volume and sentiment analysis." +
                "\n\n" +
                "### **Response Format (Strict JSON Output)**" +
                "\n- The response MUST be a **valid JSON** tailored for **Futures Trading Market Analysis** following this predefined schema." +
                "\n- **DO NOT include explanations, markdown syntax, or any text outside of the JSON structure.**" +
                "\n- Ensure the response follows this exact structure:" +
                "\n\n" +
                "**Generate the response in JSON format now.**";
    }
}
