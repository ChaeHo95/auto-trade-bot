package com.example.autotradebot.service.gemini;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class GeminiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private final CloseableHttpClient httpClient;
    private final String geminiApiUrl;

    // 요청 설정값을 필드 변수로 선언
    private final double temperature = 0.2;
    private final int topK = 40;
    private final double topP = 0.95;
    private final int maxOutputTokens = 2048;
    private final String responseMimeType = "application/json";

    @Autowired
    public GeminiService(CloseableHttpClient httpClient, @Qualifier("geminiApiUrl") String geminiApiUrl) {
        this.httpClient = httpClient;
        this.geminiApiUrl = geminiApiUrl;
    }

    /**
     * ✅ Gemini AI를 호출하여 시장 분석을 수행하는 메서드
     */
    public String requestGemini(String symbol) {
        JsonObject requestBody = createRequestBody(symbol);
        return getGeminiResponse(requestBody);
    }

    /**
     * ✅ Gemini API에 프롬프트 요청 후 응답 반환
     */
    private String getGeminiResponse(JsonObject requestBody) {
        try {
            String responseBody = sendHttpRequest(requestBody);
            return parseResponse(responseBody);
        } catch (IOException e) {
            logger.error("❌ Gemini API 호출 중 오류 발생: ", e);
            return "❌ Gemini API 오류";
        }
    }

    /**
     * ✅ HTTP 요청을 수행하고 응답을 반환하는 메서드
     */
    private String sendHttpRequest(JsonObject requestBody) throws IOException {
        HttpPost request = new HttpPost(geminiApiUrl);
        request.setHeader("Content-Type", "application/json");

        HttpEntity entity = new StringEntity(requestBody.toString(), StandardCharsets.UTF_8);
        request.setEntity(entity);

        logger.info("📤 Sending request to Gemini API: {}", requestBody);

        String response = httpClient.execute(request, responseHandler -> EntityUtils.toString(responseHandler.getEntity(), StandardCharsets.UTF_8));

        logger.info("📥 Received response from Gemini API: {}", response);
        return response;
    }

    /**
     * ✅ JSON 응답을 파싱하여 결과를 반환하는 메서드
     */
    private String parseResponse(String responseBody) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            if (!jsonResponse.has("candidates")) {
                logger.error("❌ Gemini API 응답 오류: {}", responseBody);
                return "❌ 응답 오류: candidates 필드 없음";
            }

            // candidates[0] → content → parts[0] → text 내부 JSON 추출
            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates.size() == 0) {
                logger.error("❌ Gemini API 응답 오류: candidates 배열이 비어 있음");
                return "❌ 응답 오류: candidates 배열이 비어 있음";
            }

            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
            JsonObject content = firstCandidate.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");

            if (parts.size() == 0) {
                logger.error("❌ Gemini API 응답 오류: parts 배열이 비어 있음");
                return "❌ 응답 오류: parts 배열이 비어 있음";
            }

            // "text" 내부의 JSON 문자열 가져오기
            String rawJsonString = parts.get(0).getAsJsonObject().get("text").getAsString();

            // **두 번째 JSON 파싱 (문자열을 JSON 객체로 변환)**
            JsonObject parsedResponse = JsonParser.parseString(rawJsonString).getAsJsonObject();

            // ✅ 필요한 데이터만 추출 (예시)
            String symbol = parsedResponse.get("symbol").getAsString();
            String timestamp = parsedResponse.get("timestamp").getAsString();
            String overallSentiment = parsedResponse.getAsJsonObject("analysis").get("overall_sentiment").getAsString();
            String recommendation = parsedResponse.getAsJsonObject("recommendation").get("direction").getAsString();
            String comment = parsedResponse.getAsJsonObject("recommendation").get("comment").getAsString();

            // ✅ 로깅
            logger.info("✅ Gemini 분석 결과 - Symbol: {}, Timestamp: {}, Sentiment: {}, Recommendation: {}, Comment: {}",
                    symbol, timestamp, overallSentiment, recommendation, comment);

            // JSON을 문자열로 반환
            return parsedResponse.toString();

        } catch (Exception e) {
            logger.error("❌ 응답 파싱 중 오류 발생: {}", responseBody, e);
            return "❌ 응답 파싱 오류";
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
                "\n- Retrieve and categorize **news articles** as positive, negative, or neutral based on their impact on `" + symbol + "` futures trading." +
                "\n- Analyze **tweet volume and sentiment** related to `" + symbol + "` to identify leveraged market sentiment shifts." +
                "\n- Evaluate **social media discussions (Reddit, crypto forums, etc.)** to determine leveraged trader sentiment." +
                "\n- Consider recent **macro-economic events (FOMC meetings, CPI data, interest rate hikes, regulatory announcements, etc.)** and their impact on **futures trading activity**." +
                "\n- Assess the overall **cryptocurrency futures market context (bull, bear, sideways) using Bitcoin, Ethereum, and derivatives indicators.**" +
                "\n\n" +
                "### **Futures Trading-Specific Metrics:**" +
                "\n- **Funding Rate:** Retrieve the latest funding rate for `" + symbol + "` futures and determine its impact on trader positioning." +
                "\n- **Open Interest:** Extract real-time **futures open interest** data to assess whether leveraged market participation is increasing or decreasing." +
                "\n- **Liquidation Data:** Identify **large liquidation events** (longs vs. shorts) in the futures market to detect price reversal zones." +
                "\n- **Leverage Ratio:** Analyze the **estimated leverage ratio** to determine **trader risk exposure and potential liquidation risks.**" +
                "\n- **Market Trend:** Determine whether `" + symbol + "` futures are in a **bullish, bearish, or sideways trend** based on derivatives volume and sentiment analysis." +
                "\n\n" +
                "### **Response Format (Strict JSON Output)**" +
                "\n- The response MUST be a **valid JSON** tailored for **Futures Trading Market Analysis** following this predefined schema." +
                "\n- **DO NOT include explanations, markdown syntax, or any text outside of the JSON structure.**" +
                "\n- Ensure the response follows this exact structure:" +
                "\n\n" +
                "**Generate the response in JSON format now.**";
    }
}
