package com.example.autotradebot.service.binance;

import com.example.autotradebot.config.binance.BinanceConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class BinanceService {

    private final WebClient webClient;
    private final BinanceConfig binanceConfig;

    public BinanceService(WebClient webClient, BinanceConfig binanceConfig) {
        this.webClient = webClient;
        this.binanceConfig = binanceConfig;
    }

    /**
     * Taker Buy/Sell Volume API 호출 (startTime과 endTime은 현재 시간에서 한 시간 단위로 설정)
     *
     * @param symbol 심볼 (예: "BTCUSDT")
     * @param limit  요청할 데이터 개수 (옵션, 기본값 30)
     * @return API 응답 JSON 문자열
     */
    public String getTakerBuySellVolume(String symbol, Integer limit) {
        // 현재 시간에서 한 시간 전과 현재 시간을 Unix timestamp로 변환
        long endTime = Instant.now().getEpochSecond();
        long startTime = Instant.now().minus(1, ChronoUnit.HOURS).getEpochSecond();

        String url = String.format(binanceConfig.getBinanceApiUri() + "/futures/data/takerlongshortRatio?symbol=%s&period=5m&startTime=%d&endTime=%d",
                symbol, startTime * 1000, endTime * 1000); // Unix timestamp는 밀리초로 변환해야 함

        // URL에 limit이 있으면 추가
        if (limit != null) {
            url += "&limit=" + limit;
        }

        // WebClient로 API 호출
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // API 응답을 문자열로 반환
    }

    /**
     * Long/Short Ratio API 호출 (startTime과 endTime은 현재 시간에서 한 시간 단위로 설정)
     *
     * @param symbol 심볼 (예: "BTCUSDT")
     * @param limit  요청할 데이터 개수 (옵션, 기본값 30)
     * @return API 응답 JSON 문자열
     */
    public String getLongShortRatio(String symbol, Integer limit) {
        // 현재 시간에서 한 시간 전과 현재 시간을 Unix timestamp로 변환
        long endTime = Instant.now().getEpochSecond();
        long startTime = Instant.now().minus(1, ChronoUnit.HOURS).getEpochSecond();

        String url = String.format(binanceConfig.getBinanceApiUri() + "/futures/data/globalLongShortAccountRatio?symbol=%s&period=5m&startTime=%d&endTime=%d",
                symbol, startTime * 1000, endTime * 1000); // Unix timestamp는 밀리초로 변환해야 함

        // URL에 limit이 있으면 추가
        if (limit != null) {
            url += "&limit=" + limit;
        }

        // WebClient로 API 호출
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // API 응답을 문자열로 반환
    }
}
