package com.example.autotradebot.manager;


import com.example.autotradebot.dto.TradeSignalDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TradeSignalCacheManager {

    private Logger logger = LoggerFactory.getLogger(TradeSignalCacheManager.class);

    private final ConcurrentHashMap<String, TradeSignalDto> positionCache = new ConcurrentHashMap<>();

    private List<String> symbols = new ArrayList<>();


    /**
     * 주어진 심볼에 대한 포지션 정보를 캐시에 저장합니다.
     *
     * @param symbol   트레이딩 심볼 (예: "BTCUSDT")
     * @param position 포지션 정보
     */
    public void putPosition(String symbol, TradeSignalDto position) {
        Map<String, TradeSignalDto> map = new HashMap<>();
        map.put(symbol, position);
        positionCache.put(symbol, position);
    }

    /**
     * 주어진 심볼에 대한 최신 포지션을 반환합니다.
     *
     * @param symbol 트레이딩 심볼
     * @return 해당 심볼의 포지션 정보, 없으면 null
     */
    public TradeSignalDto getPosition(String symbol) {
        return positionCache.get(symbol);
    }

    /**
     * 주어진 심볼에 대한 포지션을 삭제합니다.
     *
     * @param symbol 트레이딩 심볼
     */
    public void removePosition(String symbol, TradeSignalDto position) {
        Map<String, TradeSignalDto> map = new HashMap<>();
        map.put(symbol, position);
    }

    /**
     * 모든 포지션을 반환합니다.
     *
     * @return 모든 포지션 리스트
     */
    public ConcurrentHashMap<String, TradeSignalDto> getAllPositions() {
        return positionCache;
    }

    /**
     * 주어진 심볼에 대한 포지션이 존재하는지 확인합니다.
     *
     * @param symbol 트레이딩 심볼
     * @return 해당 심볼에 포지션이 존재하면 true, 아니면 false
     */
    public boolean hasPosition(String symbol) {
        return positionCache.containsKey(symbol);
    }


}
