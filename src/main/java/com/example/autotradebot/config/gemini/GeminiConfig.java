package com.example.autotradebot.config.gemini;

import com.example.autotradebot.config.EnvConfig;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.generativeai.ChatSession;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class GeminiConfig {

    private final EnvConfig envConfig;

    @Autowired
    public GeminiConfig(EnvConfig envConfig) {
        this.envConfig = envConfig;
    }

    /**
     * ✅ VertexAI 클라이언트 Bean 설정
     */
    @Bean
    public VertexAI vertexAiClient() throws IOException {
        String projectId = envConfig.getGeminiProjectId();
        String location = envConfig.getGeminiRegion();
        String credentialsPath = envConfig.getGeminiCredentials();
        System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", credentialsPath);
        // VertexAI 클라이언트 생성
        return new VertexAI(projectId, location);
    }

    /**
     * ✅ ChatSession Bean 설정
     */
    @Bean
    public ChatSession chatSession(VertexAI vertexAI) {
        String modelName = envConfig.getGeminiModelId();  // 예: "gemini-1.5-pro"

        // GenerativeModel 생성
        GenerationConfig generationConfig = GenerationConfig.newBuilder()
                .setTemperature(0.7F)   // 창의성 정도 (0.0 ~ 1.0)
                .setTopP(0.95F)         // Top-P 값 (0 ~ 1)
                .setTopK(40)            // Top-K 값 (토큰 수)
                .setMaxOutputTokens(2048)  // 출력 토큰의 최대 수
                .build();

        // 모델에 시스템 메시지, 사용자 메시지 및 옵션 추가
        return new GenerativeModel(modelName, vertexAI)
                .withGenerationConfig(generationConfig)  // 설정 적용
                .startChat();  // 채팅 세션 시작
    }
}
