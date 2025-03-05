package com.example.autotradebot.service.binance;

import com.example.autotradebot.dto.binance.BinanceKlineDTO;
import com.example.autotradebot.mapper.binance.BinanceKlineMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BinanceKlineService {

    private Logger logger = LoggerFactory.getLogger(BinanceKlineService.class);
    private final BinanceKlineMapper binanceKlineMapper;

    /**
     * 📌 캔들 데이터 저장
     */
    public void saveKline(BinanceKlineDTO klineDTO) {
        try {
            binanceKlineMapper.insertKline(klineDTO);
            logger.debug("✅ Kline 데이터 저장 완료: {}", klineDTO);
        } catch (Exception e) {
            logger.error("❌ Kline 데이터 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 📌 특정 심볼의 특정 시간 범위 내 Kline 데이터 조회
     */
    /**
     * 📌 특정 심볼의 특정 시간 범위 내 Kline 데이터 조회
     */
    public List<BinanceKlineDTO> getKlines(String symbol, Instant openTime, Instant closeTime) {
        return binanceKlineMapper.getKlinesWithinTimeRange(symbol,
                BigInteger.valueOf(openTime.toEpochMilli()),
                BigInteger.valueOf(closeTime.toEpochMilli()));
    }

    /**
     * 📌 특정 심볼의 최신 Kline 데이터 조회
     */
    public BinanceKlineDTO getLatestKline(String symbol) {
        return binanceKlineMapper.getLatestKline(symbol);
    }
}
