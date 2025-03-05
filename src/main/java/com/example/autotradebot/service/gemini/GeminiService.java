package com.example.autotradebot.service.gemini;

import com.example.autotradebot.config.gemini.GeminiConfig;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {
    private final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private final VertexAI vertexAI;
    private final GeminiConfig geminiConfig;

    @Autowired
    public GeminiService(VertexAI vertexAI, GeminiConfig geminiConfig) {
        this.vertexAI = vertexAI;
        this.geminiConfig = geminiConfig;
    }

    /**
     * ✅ Gemini API 호출 메서드 (비동기 방식)
     */
    public String callGeminiAiApi(String systemMessage, String userMessage) {

        return getGeminiResponse(systemMessage, userMessage);
    }

    private String getGeminiResponse(String systemMessage, String userMessage) {
        logger.info("📤 Sending request to Gemini API");

        try {
            Content systemInstruction = Content.newBuilder()
                    .setRole("system")
                    .addParts(Part.newBuilder()
                            .setText(systemMessage)
                            .build())
                    .build();

            // Send messages to the model
            GenerateContentResponse response = geminiConfig.chatSession(vertexAI)
                    .withSystemInstruction(systemInstruction).sendMessage(userMessage);

            String result = ResponseHandler.getText(response).replace("```json", "").replace("```", "").trim();
            logger.info("User message response: {}", ResponseHandler.getText(response));

            // Return the response text from the API
            return result;

        } catch (Exception e) {
            logger.error("❌ Gemini API 호출 예외 발생: ", e);
            return "❌ Gemini API 오류";
        }
    }
}
