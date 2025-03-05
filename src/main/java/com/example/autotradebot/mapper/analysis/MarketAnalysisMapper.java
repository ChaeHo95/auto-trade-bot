package com.example.autotradebot.mapper.analysis;

import com.example.autotradebot.dto.analysis.PredictionDTO;
import com.example.autotradebot.dto.binance.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.math.BigInteger;
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

    // ✅ 최근 청산 주문 데이터 조회
    List<BinanceLiquidationOrderDTO> getLiquidationOrders(@Param("symbol") String symbol, @Param("limit") int limit);

    // ✅ 최근 부분 주문서 데이터 조회
    List<BinancePartialBookDepthDTO> getPartialBookDepth(@Param("symbol") String symbol, @Param("limit") int limit);

    // ✅ 심볼과 event_time을 기준으로 binance_order_book_entries 데이터 조회
    List<BinanceOrderBookEntryDTO> getOrderBookEntriesBySymbolAndEventTime(
            @Param("symbol") String symbol,
            @Param("eventTime") BigInteger eventTime);
}
