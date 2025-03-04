package com.example.autotradebot.config.gemini;

import com.example.autotradebot.config.EnvConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {
    private final Logger logger = LoggerFactory.getLogger(GeminiConfig.class);

    private EnvConfig envConfig;

    @Autowired
    public GeminiConfig(EnvConfig envConfig) {
        this.envConfig = envConfig;
    }

    /**
     * ✅ 환경 변수에서 Gemini API Key 가져오기
     */
    private String getGeminiApiKey() {
        String apiKey = envConfig.getGeminiApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("❌ GEMINI_API_KEY가 설정되지 않았습니다!");
            throw new RuntimeException("GEMINI_API_KEY를 환경 변수 또는 .env 파일에 설정해주세요.");
        }

        logger.info("✅ Gemini API Key 로드 성공");
        return apiKey;
    }

    /**
     * ✅ 환경 변수에서 Gemini API 엔드포인트 가져오기
     */
    @Bean(name = "geminiApiUrl")
    public String geminiApiUrl() {
        String endpoint = envConfig.getGeminiApiEndpoint();

        if (endpoint == null || endpoint.isEmpty()) {
            logger.error("❌ GEMINI_API_ENDPOINT가 설정되지 않았습니다!");
            throw new RuntimeException("GEMINI_API_ENDPOINT를 환경 변수 또는 .env 파일에 설정해주세요.");
        }

        String fullUrl = endpoint + "?key=" + getGeminiApiKey();
        logger.info("✅ Gemini API Endpoint 로드 성공: {}", fullUrl);
        return fullUrl;
    }
}
