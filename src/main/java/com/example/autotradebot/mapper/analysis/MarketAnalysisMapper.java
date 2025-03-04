package com.example.autotradebot.mapper.analysis;

import com.example.autotradebot.dto.analysis.PredictionDTO;
import com.example.autotradebot.dto.binance.BinanceFundingRateDTO;
import com.example.autotradebot.dto.binance.BinanceKlineDTO;
import com.example.autotradebot.dto.binance.BinanceTradeDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface MarketAnalysisMapper {

    // ✅ 최근 200개 캔들 데이터 조회
    List<BinanceKlineDTO> getRecentKlines(@Param("symbol") String symbol, @Param("limit") int limit);

    // ✅ 최근 200개 거래 데이터 조회
    List<BinanceTradeDTO> getRecentTrades(@Param("symbol") String symbol, @Param("limit") int limit);

    // ✅ 최근 200개 펀딩 비율 데이터 조회
    List<BinanceFundingRateDTO> getFundingRates(@Param("symbol") String symbol, @Param("limit") int limit);

    // ✅ AI 차트 분석 정보 조회
    PredictionDTO getChartAnalysis(@Param("symbol") String symbol, @Param("botType") String botType);

    List<PredictionDTO> getChartAnalysisLimit(@Param("symbol") String symbol, @Param("botType") String botType, @Param("limit") int limit);

    // ✅ 이동 평균값 조회 (200개 캔들 기준)
    BigDecimal getMovingAverage(@Param("symbol") String symbol, @Param("limit") int limit);

    // ✅ RSI 값 조회 (200개 캔들 기준)
    BigDecimal getRSIValue(@Param("symbol") String symbol);

    // ✅ MACD 값 조회 (200개 캔들 기준)
    BigDecimal getMACDValue(@Param("symbol") String symbol);
}
