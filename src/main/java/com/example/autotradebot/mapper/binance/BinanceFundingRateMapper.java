package com.example.autotradebot.mapper.binance;

import com.example.autotradebot.dto.binance.BinanceFundingRateDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigInteger;
import java.util.List;

@Mapper
public interface BinanceFundingRateMapper {

    /**
     * ✅ Funding Rate 데이터 저장
     * 중복 데이터가 존재할 경우, 최신 데이터로 업데이트
     *
     * @param fundingRateDTO 저장할 Funding Rate 데이터 객체
     */
    void insertFundingRate(BinanceFundingRateDTO fundingRateDTO);

    /**
     * ✅ 특정 심볼의 최신 Funding Rate 조회
     *
     * @param symbol 거래 심볼 (예: BTCUSDT)
     * @return 해당 심볼의 가장 최근 Funding Rate 데이터
     */
    BinanceFundingRateDTO getLatestFundingRate(@Param("symbol") String symbol);

    /**
     * ✅ 특정 심볼의 특정 기간 동안 Funding Rate 조회
     *
     * @param symbol 거래 심볼 (예: BTCUSDT)
     * @param startTime 조회 시작 시간 (Unix Timestamp, 밀리초)
     * @param endTime 조회 종료 시간 (Unix Timestamp, 밀리초)
     * @return 해당 심볼의 특정 기간 동안 Funding Rate 데이터 리스트
     */
    List<BinanceFundingRateDTO> getFundingRatesByTimeRange(
            @Param("symbol") String symbol,
            @Param("startTime") BigInteger startTime,
            @Param("endTime") BigInteger endTime
    );

    /**
     * ✅ 최신 Funding Rate 데이터 목록 조회 (최근 100개)
     *
     * @return 최신 Funding Rate 데이터 리스트 (최대 100개)
     */
    List<BinanceFundingRateDTO> getRecentFundingRates();

    List<BinanceFundingRateDTO> getFundingRatesInRange(String symbol, long startTime, long endTime);

}
