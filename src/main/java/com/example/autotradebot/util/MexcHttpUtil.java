package com.example.autotradebot.util;

import com.google.gson.Gson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class MexcHttpUtil {
    private static final Gson gson = new Gson();
    private final WebClient webClient;

    public MexcHttpUtil(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 📌 서명(Signature) 포함한 GET 요청 (비동기)
     */
    public Mono<String> sendSignedGetRequest(String url, Map<String, String> params, String apiKey, String secretKey) {
        try {
            long timestamp = System.currentTimeMillis();
            params.put("timestamp", String.valueOf(timestamp));

            // ✅ 서명 생성
            String signature = MexcSignatureUtil.generateSignature(params, secretKey);
            params.put("signature", signature);

            return webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(url);
                        params.forEach(uriBuilder::queryParam);
                        return uriBuilder.build();
                    })
                    .header("X-MEXC-APIKEY", apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(error -> System.err.println("❌ GET 요청 실패: " + error.getMessage())); // 오류 처리 추가
        } catch (Exception e) {
            return Mono.error(new RuntimeException("❌ 서명 생성 실패: " + e.getMessage(), e));
        }
    }

    /**
     * 📌 서명(Signature) 포함한 POST 요청 (비동기)
     */
    public Mono<String> sendSignedPostRequest(String url, Map<String, Object> payload, String apiKey, String secretKey) {
        try {
            long timestamp = System.currentTimeMillis();
            payload.put("timestamp", timestamp);

            // ✅ 서명 생성
            String signature = MexcSignatureUtil.generateSignature(payload, secretKey);
            payload.put("signature", signature);

            String jsonBody = gson.toJson(payload); // JSON 변환

            return webClient.post()
                    .uri(url)
                    .header("X-MEXC-APIKEY", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(error -> System.err.println("❌ POST 요청 실패: " + error.getMessage())); // 오류 처리 추가
        } catch (Exception e) {
            return Mono.error(new RuntimeException("❌ 서명 생성 실패: " + e.getMessage(), e));
        }
    }
}
