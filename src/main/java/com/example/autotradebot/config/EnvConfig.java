package com.example.autotradebot.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {
    private final Logger logger = LoggerFactory.getLogger(EnvConfig.class);
    private Dotenv dotenv = Dotenv.load();

    public EnvConfig() {
        // ✅ 실행 환경 (local, dev, prod)을 가져오기
        String envProfile = dotenv.get("ENV_PROFILE");
        if (envProfile == null || envProfile.isEmpty()) {
            envProfile = "local";  // 기본값을 local로 설정
        }

        // ✅ 환경별 `.env` 파일 로드 (.env.local, .env.dev, .env.prod)
        dotenv = Dotenv.configure()
                .filename(".env." + envProfile)  // 예: ".env.local"
                .ignoreIfMissing()
                .load();

        logger.info("✅ 환경 설정 파일 로드 완료: .env.{}", envProfile);
    }

    @Bean
    public String getDbUsername() {
        return getEnvVariable("MYSQL_DB_USERNAME");
    }

    @Bean
    public String getDbPassword() {
        return getEnvVariable("MYSQL_DB_PASSWORD");
    }

    @Bean
    public String getDbUrl() {
        return getEnvVariable("MYSQL_DB_URL");
    }

    @Bean
    public String getGeminiProjectId() {
        return getEnvVariable("GEMINI_PROJECT_ID");
    }

    @Bean
    public String getGeminiRegion() {
        return getEnvVariable("GEMINI_REGION");
    }

    @Bean
    public String getGeminiModelId() {
        return getEnvVariable("GEMINI_MODEL_ID");
    }

    @Bean
    public String getGeminiCredentials() {
        return getEnvVariable("GOOGLE_APPLICATION_CREDENTIALS");
    }

    @Bean
    public String getBinanceWsUrl() {
        return getEnvVariable("BINANCE_WS_URL");
    }

    @Bean
    public String getBinanceBaseUrl() {
        return getEnvVariable("BINANCE_BASE_URL");
    }

    @Bean
    public String getOpenAiApiKey() {
        return getEnvVariable("OPENAI_API_KEY");
    }

    @Bean
    public String getOpenAiApiEndpoint() {
        return getEnvVariable("OPENAI_API_ENDPOINT");
    }

    @Bean
    public String getLogPath() {
        return getEnvVariable("LOG_PATH");
    }

    private String getEnvVariable(String key) {
        String value = dotenv.get(key);
        if (value == null || value.isEmpty()) {
            logger.error("❌ {} 환경 변수가 설정되지 않았습니다!", key);
            return null;
        } else {
            logger.info("✅ {} 로드 성공: {}", key);
            return value;
        }
    }
}
