package com.example.autotradebot.mapper.binance;

import com.example.autotradebot.dto.binance.BinanceTradeDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BinanceTradeMapper {

    // ✅ Trade 데이터 저장
    void insertTrade(BinanceTradeDTO trade);

    // ✅ 특정 심볼의 최신 거래 데이터 가져오기 (최근 10개)
    List<BinanceTradeDTO> getLatestTrades(@Param("symbol") String symbol);
}
