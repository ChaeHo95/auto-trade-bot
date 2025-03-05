package com.example.autotradebot.mapper.binance;

import com.example.autotradebot.dto.binance.BinanceLiquidationOrderDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BinanceLiquidationOrderMapper {
    /**
     * 📌 강제 청산 주문 데이터 저장
     */
    void insertLiquidationOrder(BinanceLiquidationOrderDTO liquidationOrderDTO);

    /**
     * 📌 특정 심볼의 최신 강제 청산 주문 조회
     */
    BinanceLiquidationOrderDTO getLatestLiquidationOrder(@Param("symbol") String symbol);
}
