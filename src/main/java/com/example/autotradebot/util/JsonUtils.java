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
        // Null 체크: key가 null이면 IllegalArgumentException을 던짐
        if (key == null) {
            throw new IllegalArgumentException("The key cannot be null.");
        }

        // JsonObject에서 key가 존재하고 null이 아니면 값을 가져옴
        if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
            try {
                String value = jsonObject.get(key).getAsString();

                // 빈 문자열 처리: 빈 문자열이면 "N/A" 반환
                return value.isEmpty() ? "N/A" : value;
            } catch (ClassCastException e) {
                // 값이 문자열이 아닐 경우 처리
                System.err.println("❌ The value for key '" + key + "' is not a string.");
                return "N/A"; // 기본값 반환
            }
        }

        // key가 없거나 null일 경우 기본값 반환
        return "N/A";
    }

    public BigDecimal getBigDecimal(JsonObject jsonObject, String key) {
        if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
            String value = jsonObject.get(key).getAsString().trim();

            // "NULL" 문자열 체크 및 숫자로 변환할 수 없는 경우 0 반환
            if ("NULL".equalsIgnoreCase(value) || !value.matches("-?\\d+(\\.\\d+)?")) {
                return BigDecimal.ZERO;
            }

            return new BigDecimal(value);
        }
        return BigDecimal.ZERO; // 기본값
    }

    public BigInteger getBigInteger(JsonObject jsonObject, String key) {
        if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
            String value = jsonObject.get(key).getAsString().trim();

            // "NULL" 문자열 체크 및 숫자로 변환할 수 없는 경우 0 반환
            if ("NULL".equalsIgnoreCase(value) || !value.matches("-?\\d+")) {
                return BigInteger.ZERO;
            }

            return new BigInteger(value);
        }
        return BigInteger.ZERO; // 기본값
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
            if (element.isJsonNull() || element.getAsString().trim().isEmpty() || element.getAsString().equalsIgnoreCase("[current timestamp]")) {
                System.err.println("❌ Invalid timestamp (null, empty, or invalid value) for key: " + key + " → 현재 시간 반환");
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
