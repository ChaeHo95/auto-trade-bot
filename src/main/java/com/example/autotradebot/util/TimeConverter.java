package com.example.autotradebot.util;

import org.springframework.stereotype.Component;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class TimeConverter {
    public static LocalDateTime convertToDateTime(BigInteger timestamp) {
        return Instant.ofEpochMilli(timestamp.longValue())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
