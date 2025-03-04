package com.example.autotradebot.util;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MexcSignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * 📌 HMAC-SHA256 서명 생성 (문자열 데이터)
     */
    private static String generateSignature(String data, String secretKey) throws Exception {
        Mac sha256HMAC = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        sha256HMAC.init(secretKeySpec);
        byte[] hash = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));

        return String.format("%064x", new BigInteger(1, hash)); // 16진수 변환 (BigInteger 사용)
    }

    /**
     * 📌 HMAC-SHA256 서명 생성 (쿼리 파라미터 Map)
     */
    public static String generateSignature(Map<String, ?> params, String secretKey) throws Exception {
        String queryString = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .sorted()
                .collect(Collectors.joining("&")); // 파라미터를 정렬하여 생성

        return generateSignature(queryString, secretKey);
    }
}
