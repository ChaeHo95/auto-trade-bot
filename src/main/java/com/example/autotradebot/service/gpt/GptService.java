package com.example.autotradebot.service.gpt;

import com.example.autotradebot.dto.bot.OpenAiResponseDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GptService {

    private Logger logger = LoggerFactory.getLogger(GptService.class);
    private final WebClient webClient;

    @Autowired
    public GptService(@Qualifier("openAiApiClient") WebClient openAiApiClient) {
        this.webClient = openAiApiClient;
    }


    /**
     * WebClient를 이용하여 OpenAI API 호출
     */
    public String callOpenAiApi(String systemMessage, String userMessage) {
        try {
            // Create request body
            JsonObject requestBodyJson = new JsonObject();
            requestBodyJson.addProperty("model", "gpt-4o");

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

                    return cleanedResponseText;
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
}
