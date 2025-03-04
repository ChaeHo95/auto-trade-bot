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

    private final double temperature = 0.7;
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
     * ✅ Gemini API 호출 메서드 (비동기 방식)
     */
    public String callGeminiAiApi(String systemMessage, String userMessage) {
        JsonObject requestBody = createRequestBody(systemMessage, userMessage);
        return getGeminiResponse(requestBody);
    }
    
    /**
     * ✅ Gemini API에 프롬프트 요청 후 응답 반환 (비동기 방식)
     */
    private String getGeminiResponse(JsonObject requestBody) {
        logger.info("📤 Sending request to Gemini API: {}", requestBody);

        try {
            String response = webClient.post()
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(res -> logger.info("📥 Received response from Gemini API: {}", res))
                    .onErrorResume(error -> {
                        logger.error("❌ Gemini API 호출 중 오류 발생: {}", error.getMessage());
                        return Mono.just("❌ Gemini API 오류");
                    })
                    .block(); // ✅ 동기 실행

            // ✅ 여기서 parseResponse 실행 후 결과 리턴
            return parseResponse(response).block();

        } catch (Exception e) {
            logger.error("❌ Gemini API 호출 예외 발생: ", e);
            return "❌ Gemini API 오류";
        }
    }

    /**
     * ✅ JSON 응답을 파싱하여 'text' 필드만 반환하는 메서드 (비동기 방식)
     */
    private Mono<String> parseResponse(String responseBody) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                JsonObject content = firstCandidate.getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    return Mono.just(parts.get(0).getAsJsonObject().get("text").getAsString());
                }
            }
            return Mono.just("❌ 응답에 'text' 필드가 없음");
        } catch (Exception e) {
            logger.error("❌ 응답 파싱 중 오류 발생: {} 에러 메시지: {}", responseBody, e.getMessage(), e);
            return Mono.error(new RuntimeException("❌ 응답 파싱 오류: " + e.getMessage()));
        }
    }

    /**
     * ✅ JSON 기반 요청 본문 생성 (프롬프트 포함)
     */
    private JsonObject createRequestBody(String systemMessage, String userMessage) {
        JsonObject requestBody = new JsonObject();

        // 시스템 메시지 추가
        JsonObject systemInstruction = new JsonObject();
        JsonArray systemPartsArray = new JsonArray();
        JsonObject systemTextObject = new JsonObject();
        systemTextObject.addProperty("text", systemMessage);
        systemPartsArray.add(systemTextObject);
        systemInstruction.add("parts", systemPartsArray);
        requestBody.add("system_instruction", systemInstruction);

        // 사용자 메시지 추가
        JsonArray contentsArray = new JsonArray();
        JsonObject userMessageObject = new JsonObject();
        userMessageObject.addProperty("role", "user");
        JsonArray userPartsArray = new JsonArray();
        JsonObject userTextObject = new JsonObject();
        userTextObject.addProperty("text", userMessage);
        userPartsArray.add(userTextObject);
        userMessageObject.add("parts", userPartsArray);
        contentsArray.add(userMessageObject);
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
}
