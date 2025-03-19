package com.example.autotradebot.manager;


import com.example.autotradebot.dto.TradeSignalDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TradeSignalCacheManager {

    private Logger logger = LoggerFactory.getLogger(TradeSignalCacheManager.class);

    private final ConcurrentHashMap<String, TradeSignalDto> tradeSignalCache = new ConcurrentHashMap<>();


    /**
     * 주어진 심볼에 대한 포지션 정보를 캐시에 저장합니다.
     *
     * @param symbol      트레이딩 심볼 (예: "BTCUSDT")
     * @param tradeSignal 포지션 정보
     */
    public void putTradeSignal(String symbol, TradeSignalDto tradeSignal) {
        Map<String, TradeSignalDto> map = new HashMap<>();
        map.put(symbol, tradeSignal);
        tradeSignalCache.put(symbol, tradeSignal);
    }

    /**
     * 모든 포지션을 반환합니다.
     *
     * @return 모든 포지션 리스트
     */
    public ConcurrentHashMap<String, TradeSignalDto> getAllTradeSignals() {
        return tradeSignalCache;
    }

}
