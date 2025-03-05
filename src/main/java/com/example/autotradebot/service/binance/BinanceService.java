package com.example.autotradebot.service.binance;

import com.example.autotradebot.config.binance.BinanceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class BinanceService {

    private final WebClient webClient;
    private final BinanceConfig binanceConfig;

    @Autowired
    public BinanceService(WebClient webClient, BinanceConfig binanceConfig) {
        this.webClient = webClient;
        this.binanceConfig = binanceConfig;
    }

    /**
     * Taker Buy/Sell Volume API 호출
     *
     * @param symbol    심볼 (예: "BTCUSDT")
     * @param period    기간 (예: "1h")
     * @param limit     요청할 데이터 개수 (옵션, 기본값 30)
     * @param startTime 시작 시간 (옵션)
     * @param endTime   종료 시간 (옵션)
     * @return API 응답 JSON 문자열
     */
    public Mono<String> getTakerBuySellVolume(String symbol, String period, Integer limit, Long startTime, Long endTime) {
        String url = String.format("/futures/data/takerlongshortRatio?symbol=%s&period=%s", symbol, period);

        // URL에 추가적인 파라미터가 있다면 추가
        if (limit != null) {
            url += "&limit=" + limit;
        }
        if (startTime != null) {
            url += "&startTime=" + startTime;
        }
        if (endTime != null) {
            url += "&endTime=" + endTime;
        }

        // WebClient로 API 호출
        return webClient.get()
                .uri(binanceConfig.getBinanceApiUri() + url)
                .retrieve()
                .bodyToMono(String.class); // API 응답을 문자열로 반환
    }

    /**
     * Long/Short Ratio API 호출
     *
     * @param symbol    심볼 (예: "BTCUSDT")
     * @param period    기간 (예: "1h")
     * @param limit     요청할 데이터 개수 (옵션, 기본값 30)
     * @param startTime 시작 시간 (옵션)
     * @param endTime   종료 시간 (옵션)
     * @return API 응답 JSON 문자열
     */
    public Mono<String> getLongShortRatio(String symbol, String period, Integer limit, Long startTime, Long endTime) {
        String url = String.format("/futures/data/globalLongShortAccountRatio?symbol=%s&period=%s", symbol, period);

        // URL에 추가적인 파라미터가 있다면 추가
        if (limit != null) {
            url += "&limit=" + limit;
        }
        if (startTime != null) {
            url += "&startTime=" + startTime;
        }
        if (endTime != null) {
            url += "&endTime=" + endTime;
        }

        // WebClient로 API 호출
        return webClient.get()
                .uri(binanceConfig.getBinanceApiUri() + url)
                .retrieve()
                .bodyToMono(String.class); // API 응답을 문자열로 반환
    }
}
