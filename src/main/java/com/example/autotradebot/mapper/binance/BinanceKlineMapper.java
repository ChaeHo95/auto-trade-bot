package com.example.autotradebot.mapper.binance;

import com.example.autotradebot.dto.binance.BinanceKlineDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigInteger;
import java.util.List;

@Mapper
public interface BinanceKlineMapper {

    // ✅ Kline 데이터 저장
    void insertKline(BinanceKlineDTO kline);

    // ✅ 특정 심볼 & 특정 시간 범위 Kline 조회
    List<BinanceKlineDTO> getKlinesWithinTimeRange(
            @Param("symbol") String symbol,
            @Param("openTime") BigInteger openTime,
            @Param("closeTime") BigInteger closeTime
    );

    // ✅ 최신 Kline 데이터 가져오기
    BinanceKlineDTO getLatestKline(@Param("symbol") String symbol);

}
