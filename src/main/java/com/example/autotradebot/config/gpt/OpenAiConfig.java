package com.example.autotradebot.config.gpt;

import com.example.autotradebot.config.EnvConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    private Logger logger = LoggerFactory.getLogger(OpenAiConfig.class);

    private EnvConfig envConfig;

    @Autowired
    public OpenAiConfig(EnvConfig envConfig) {
        this.envConfig = envConfig;
    }

    /**
     * ✅ OpenAI API 키 가져오기
     */
    private String getOpenAiApiKey() {
        String apiKey = envConfig.getOpenAiApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("❌ OpenAI API 키가 설정되지 않았습니다!");
            throw new RuntimeException("OpenAI API 키를 설정해주세요 (환경 변수 또는 .env 파일).");
        }

        logger.info("✅ OpenAI API 키가 정상적으로 로드되었습니다.");
        return apiKey;
    }

    /**
     * ✅ OpenAI API 엔드포인트 가져오기 (커스텀 가능)
     */
    private String getOpenAiApiEndpoint() {
        String endpoint = envConfig.getOpenAiApiEndpoint();

        logger.info("✅ OpenAI 엔드포인트 설정됨: {}", endpoint);
        return endpoint;
    }

    /**
     * ✅ OpenAI API 클라이언트 Bean 등록
     */
    @Bean
    public OpenAiApi openAiApi() {
        String apiKey = getOpenAiApiKey();
        String endpoint = getOpenAiApiEndpoint();

        // Use the constructor that uses API Key and Endpoint
        return new OpenAiApi(endpoint, apiKey);
    }

    /**
     * ✅ OpenAI Chat Model Bean 등록
     */
    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        return new OpenAiChatModel(openAiApi);
    }
}
