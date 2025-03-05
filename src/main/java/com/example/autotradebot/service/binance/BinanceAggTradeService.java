package com.example.autotradebot.service.binance;

import com.example.autotradebot.dto.binance.BinanceAggTradeDTO;
import com.example.autotradebot.mapper.binance.BinanceAggTradeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BinanceAggTradeService {

    private final BinanceAggTradeMapper aggTradeMapper;
    private static final Logger logger = LoggerFactory.getLogger(BinanceAggTradeService.class);

    public BinanceAggTradeService(BinanceAggTradeMapper aggTradeMapper) {
        this.aggTradeMapper = aggTradeMapper;
    }

    /**
     * ✅ Aggregate Trade 저장
     */
    public void saveAggTrade(BinanceAggTradeDTO aggTradeDTO) {
        try {
            aggTradeMapper.insertAggTrade(aggTradeDTO);
            logger.debug("📊 Aggregate Trade 저장됨: {}", aggTradeDTO);
        } catch (Exception e) {
            logger.error("❌ Aggregate Trade 저장 오류: ", e);
        }
    }

    /**
     * ✅ 최신 Aggregate Trade 조회
     */
    public BinanceAggTradeDTO getLatestAggTrade(String symbol) {
        return aggTradeMapper.getLatestAggTrade(symbol);
    }
}
