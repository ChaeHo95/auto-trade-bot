package com.example.autotradebot.service.binance;

import com.example.autotradebot.dto.binance.BinanceLiquidationOrderDTO;
import com.example.autotradebot.mapper.binance.BinanceLiquidationOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BinanceLiquidationOrderService {

    private final Logger logger = LoggerFactory.getLogger(BinanceLiquidationOrderService.class);
    private final BinanceLiquidationOrderMapper binanceLiquidationOrderMapper;

    public BinanceLiquidationOrderService(BinanceLiquidationOrderMapper binanceLiquidationOrderMapper) {
        this.binanceLiquidationOrderMapper = binanceLiquidationOrderMapper;
    }

    /**
     * 📌 강제 청산 주문 데이터 저장
     */
    public void saveLiquidationOrder(BinanceLiquidationOrderDTO liquidationOrderDTO) {
        try {
            binanceLiquidationOrderMapper.insertLiquidationOrder(liquidationOrderDTO);
            logger.debug("✅ 강제 청산 주문 저장 완료: {}", liquidationOrderDTO);
        } catch (Exception e) {
            logger.error("❌ 강제 청산 주문 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 📌 특정 심볼의 최신 강제 청산 주문 조회
     */
    public BinanceLiquidationOrderDTO getLatestLiquidationOrder(String symbol) {
        return binanceLiquidationOrderMapper.getLatestLiquidationOrder(symbol);
    }
}
