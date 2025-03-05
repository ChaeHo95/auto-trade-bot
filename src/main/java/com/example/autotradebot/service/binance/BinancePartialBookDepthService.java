package com.example.autotradebot.service.binance;

import com.example.autotradebot.dto.binance.BinancePartialBookDepthDTO;
import com.example.autotradebot.mapper.binance.BinancePartialBookDepthMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
public class BinancePartialBookDepthService {

    private final Logger logger = LoggerFactory.getLogger(BinancePartialBookDepthService.class);
    private final BinancePartialBookDepthMapper binancePartialBookDepthMapper;

    public BinancePartialBookDepthService(BinancePartialBookDepthMapper binancePartialBookDepthMapper) {
        this.binancePartialBookDepthMapper = binancePartialBookDepthMapper;
    }

    /**
     * 📌 호가 데이터 저장
     */
    public void savePartialBookDepth(BinancePartialBookDepthDTO bookDepth) {
        try {
            // ✅ Partial Book Depth 저장 (transaction_time을 기반으로 저장)
            binancePartialBookDepthMapper.insertPartialBookDepth(bookDepth);
            BigInteger transactionTime = bookDepth.getTransactionTime(); // ✅ 트랜잭션 시간 사용

            // ✅ Order Book Entries 저장
            if (!bookDepth.getBids().isEmpty() || !bookDepth.getAsks().isEmpty()) {
                binancePartialBookDepthMapper.insertOrderBookEntries(transactionTime, bookDepth.getBids(), bookDepth.getAsks());
            }

            logger.debug("📊 호가 데이터 저장 완료 (transactionTime={}): {}", transactionTime, bookDepth);
        } catch (Exception e) {
            logger.error("❌ 호가 데이터 저장 오류: ", e);
        }
    }

    /**
     * 📌 특정 심볼의 최신 호가 데이터 조회
     */
    public BinancePartialBookDepthDTO getLatestPartialBookDepth(String symbol) {
        return binancePartialBookDepthMapper.getLatestPartialBookDepth(symbol);
    }
}
