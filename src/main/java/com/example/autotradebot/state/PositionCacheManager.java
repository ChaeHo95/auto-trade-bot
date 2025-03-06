package com.example.autotradebot.state;

import com.example.autotradebot.dto.analysis.Position;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component

public class PositionCacheManager {
    // 심볼별로 포지션을 관리하는 ConcurrentHashMap
    private final ConcurrentHashMap<String, Position> positionCache = new ConcurrentHashMap<>();

    /**
     * 주어진 심볼에 대한 포지션 정보를 캐시에 저장합니다.
     *
     * @param symbol   트레이딩 심볼 (예: "BTCUSDT")
     * @param position 포지션 정보
     */
    public void putPosition(String symbol, Position position) {
        positionCache.put(symbol, position);
    }

    /**
     * 주어진 심볼에 대한 최신 포지션을 반환합니다.
     *
     * @param symbol 트레이딩 심볼
     * @return 해당 심볼의 포지션 정보, 없으면 null
     */
    public Position getPosition(String symbol) {
        return positionCache.get(symbol);
    }

    /**
     * 주어진 심볼에 대한 포지션을 삭제합니다.
     *
     * @param symbol 트레이딩 심볼
     */
    public void removePosition(String symbol) {
        positionCache.remove(symbol);
    }

    /**
     * 주어진 심볼에 대한 포지션 상태를 업데이트합니다.
     * - "WAIT" 상태가 들어오면 무시
     * - "EXIT" 상태가 들어오면 "WAIT" 상태로 변경
     *
     * @param symbol    트레이딩 심볼
     * @param newStatus 새로운 상태
     */
    public void updatePositionStatus(String symbol, String newStatus) {
        Position position = positionCache.get(symbol);

        if (position != null) {
            if ("WAIT".equals(newStatus)) {
                // "WAIT" 상태는 무시
                return;
            } else if ("EXIT".equals(newStatus)) {
                // "EXIT" 상태는 "WAIT"로 변경
                position.setPositionStatus("WAIT");
                putPosition(symbol, position);  // 상태 변경 후 다시 저장
            } else {
                putPosition(symbol, position);
            }
        }
    }

    /**
     * 모든 포지션을 반환합니다.
     *
     * @return 모든 포지션 리스트
     */
    public ConcurrentHashMap<String, Position> getAllPositions() {
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
