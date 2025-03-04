package com.example.autotradebot.mapper.binance;

import com.example.autotradebot.dto.binance.BinanceAggTradeDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BinanceAggTradeMapper {

    void insertAggTrade(BinanceAggTradeDTO aggTradeDTO);

    BinanceAggTradeDTO getLatestAggTrade(String symbol);
}
