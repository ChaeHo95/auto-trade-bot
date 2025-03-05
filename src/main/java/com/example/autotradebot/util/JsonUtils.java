package com.example.autotradebot.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class JsonUtils {

    /**
     * JSON에서 값이 존재하는지 확인하고 값이 존재하면 반환, 없으면 기본값 반환
     *
     * @param jsonObject - JsonObject 객체
     * @param key        - 키 이름
     * @return 값이 존재하면 해당 값, 없으면 기본값
     */
    public String getString(JsonObject jsonObject, String key) {
        return jsonObject.has(key) && !jsonObject.get(key).isJsonNull()
                ? jsonObject.get(key).getAsString()
                : "N/A";
    }

    public BigDecimal getBigDecimal(JsonObject jsonObject, String key) {
        return jsonObject.has(key) && !jsonObject.get(key).isJsonNull()
                ? new BigDecimal(jsonObject.get(key).getAsString())
                : BigDecimal.ZERO;
    }

    public BigInteger getBigInteger(JsonObject jsonObject, String key) {
        return jsonObject.has(key) && !jsonObject.get(key).isJsonNull()
                ? new BigInteger(jsonObject.get(key).getAsString())
                : BigInteger.ZERO;
    }

    public static LocalDateTime getLocalDateTime(JsonObject jsonObject, String key) {
        try {
            // JSON에서 key 존재 여부 확인
            if (!jsonObject.has(key)) {
                System.err.println("❌ JSON 객체에 해당 키 없음: " + key + " → 현재 시간 반환");
                return LocalDateTime.now(); // 현재 시간 반환
            }

            JsonElement element = jsonObject.get(key);

            // 값이 null이거나 빈 문자열인지 확인
            if (element.isJsonNull() || element.getAsString().trim().isEmpty()) {
                System.err.println("❌ Invalid timestamp (null or empty) for key: " + key + " → 현재 시간 반환");
                return LocalDateTime.now(); // 현재 시간 반환
            }

            String timestamp = element.getAsString();
            System.out.println("✅ Parsing timestamp for key '" + key + "': " + timestamp);

            // 날짜 포맷 지정 및 변환
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            return LocalDateTime.parse(timestamp, formatter);
        } catch (DateTimeParseException e) {
            System.err.println("❌ Cannot parse timestamp for key '" + key + "': " + jsonObject.get(key) + " → 현재 시간 반환");
            return LocalDateTime.now(); // 변환 실패 시 현재 시간 반환
        }
    }
}
