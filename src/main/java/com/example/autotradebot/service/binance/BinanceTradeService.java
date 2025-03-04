package com.example.autotradebot.service.binance;

import com.example.autotradebot.dto.binance.BinanceTradeDTO;
import com.example.autotradebot.mapper.binance.BinanceTradeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BinanceTradeService {

    private Logger logger = LoggerFactory.getLogger(BinanceTradeService.class);
    private final BinanceTradeMapper binanceTradeMapper;

    public BinanceTradeService(BinanceTradeMapper binanceTradeMapper) {
        this.binanceTradeMapper = binanceTradeMapper;
    }

    /**
     * 📌 거래 데이터 저장
     */
    public void saveTrade(BinanceTradeDTO tradeDTO) {
        try {
            binanceTradeMapper.insertTrade(tradeDTO);
            logger.info("✅ Trade 데이터 저장 완료: {}", tradeDTO);
        } catch (Exception e) {
            logger.error("❌ Trade 데이터 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 📌 특정 심볼의 최근 10개 거래 조회
     */
    public List<BinanceTradeDTO> getLatestTrades(String symbol) {
        return binanceTradeMapper.getLatestTrades(symbol);
    }
}
