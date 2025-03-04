package com.example.autotradebot.util;

import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class MexcHttpUtil {
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .protocols(java.util.Collections.singletonList(Protocol.HTTP_1_1)) // 📌 HTTP/1.1 강제 사용
            .connectTimeout(30, TimeUnit.SECONDS) // 📌 연결 타임아웃 설정
            .readTimeout(30, TimeUnit.SECONDS) // 📌 응답 대기 시간 설정
            .writeTimeout(30, TimeUnit.SECONDS) // 📌 요청 타임아웃 설정
            .retryOnConnectionFailure(true) // 📌 연결 실패 시 재시도
            .build();

    // 📌 서명(Signature) 포함한 GET 요청
    public String sendSignedGetRequest(String url, Map<String, String> params, String apiKey, String secretKey) throws IOException {
        long timestamp = System.currentTimeMillis();
        params.put("timestamp", String.valueOf(timestamp));

        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            queryString.append(param.getKey()).append("=").append(param.getValue()).append("&");
        }
        queryString.setLength(queryString.length() - 1); // 마지막 `&` 제거

        // 📌 서명 생성
        String signature;
        try {
            signature = MexcSignatureUtil.generateSignature(queryString.toString(), secretKey);
        } catch (Exception e) {
            throw new IOException("Signature creation failed", e);
        }

        HttpUrl signedUrl = HttpUrl.parse(url).newBuilder()
                .addQueryParameter("timestamp", String.valueOf(timestamp))
                .addQueryParameter("signature", signature)
                .build();

        Request request = new Request.Builder()
                .url(signedUrl)
                .addHeader("X-MEXC-APIKEY", apiKey)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
            return response.body().string();
        }
    }

    // 📌 서명(Signature) 포함한 POST 요청
    public String sendSignedPostRequest(String url, String jsonBody, String apiKey, String secretKey) throws IOException {
        long timestamp = System.currentTimeMillis();
        String payload = "timestamp=" + timestamp;

        // 📌 서명 생성
        String signature;
        try {
            signature = MexcSignatureUtil.generateSignature(payload, secretKey);
        } catch (Exception e) {
            throw new IOException("Signature creation failed", e);
        }

        HttpUrl signedUrl = HttpUrl.parse(url).newBuilder()
                .addQueryParameter("timestamp", String.valueOf(timestamp))
                .addQueryParameter("signature", signature)
                .build();

        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(signedUrl)
                .addHeader("X-MEXC-APIKEY", apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
            return response.body().string();
        }
    }
}
