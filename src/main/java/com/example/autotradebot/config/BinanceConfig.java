package com.example.autotradebot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BinanceConfig {

    private Logger logger = LoggerFactory.getLogger(BinanceConfig.class);
    private final EnvConfig envConfig;

    @Autowired
    public BinanceConfig(EnvConfig envConfig) {
        this.envConfig = envConfig;
    }

    public ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer ->
                    configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
            .build();


    public WebClient binanceApiClient() {
        String endpoint = envConfig.getBinanceApiEndpoint();

        if (endpoint == null || endpoint.isEmpty()) {
            logger.error("❌ BINANCE_API_ENDPOINT가 설정되지 않았습니다!");
            throw new RuntimeException("BINANCE_API_ENDPOINT를 환경 변수 또는 .env 파일에 설정해주세요.");
        }

        return WebClient.builder()
                .baseUrl(endpoint)
                .exchangeStrategies(strategies)
                .build();
    }
}

