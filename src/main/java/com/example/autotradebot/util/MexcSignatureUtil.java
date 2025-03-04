package com.example.autotradebot.util;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
public class MexcSignatureUtil {
    public static String generateSignature(String data, String secretKey) throws Exception {
        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
        sha256HMAC.init(secretKeySpec);
        byte[] hash = sha256HMAC.doFinal(data.getBytes());

        // 📌 16진수(Hex) 변환 (Base64 대신)
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0'); // 한 자리 숫자는 앞에 0 추가
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
