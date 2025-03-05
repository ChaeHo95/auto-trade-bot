package com.example.autotradebot.service.binance;

import com.example.autotradebot.dto.binance.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@Component
public class BinanceWebSocketClient extends WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(BinanceWebSocketClient.class);
    private final BinanceKlineService klineService;
    private final BinanceTickerService tickerService;
    private final BinanceTradeService tradeService;
    private final BinanceFundingRateService fundingRateService;
    private final BinanceAggTradeService aggTradeService;
    private final ObjectMapper objectMapper;

    // ✅ 재연결 관련 변수
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long RECONNECT_DELAY = TimeUnit.SECONDS.toMillis(5);
    private int reconnectAttempts = 0;


    @Value("${enable.binance.websocket}") // 기본값 false
    private boolean enableWebSocket;

    /**
     * ✅ Binance WebSocketClient 생성자
     */
    public BinanceWebSocketClient(URI serverUri,
                                  BinanceKlineService klineService,
                                  BinanceTickerService tickerService,
                                  BinanceTradeService tradeService,
                                  BinanceFundingRateService fundingRateService,
                                  BinanceAggTradeService aggTradeService,
                                  ObjectMapper objectMapper) {
        super(serverUri);
        this.klineService = klineService;
        this.tickerService = tickerService;
        this.tradeService = tradeService;
        this.fundingRateService = fundingRateService;
        this.aggTradeService = aggTradeService;
        this.objectMapper = objectMapper;
    }

    /**
     * ✅ WebSocket 상태 체크 및 자동 재연결 (5분마다 실행)
     */
    @Scheduled(fixedDelay = 1 * 60 * 1000)
    public void checkAndReconnect() {
        if (!enableWebSocket) {
            logger.debug("⚠ WebSocket 실행이 비활성화됨.");
            return; // 실행하지 않음
        }


        if (!isWebSocketOpen()) {
            logger.warn("⚠️ WebSocket이 닫혀 있음. 재연결 시도...");
            reconnectWithDelay();
        } else {
            logger.info("✅ WebSocket 정상 작동 중.");
        }
    }

    /**
     * ✅ WebSocket 연결 성공 시 호출
     */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("✅ Binance WebSocket 연결 성공!");
        reconnectAttempts = 0;
    }

    /**
     * ✅ WebSocket 메시지 수신 처리
     */
    @Override
    public void onMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            // ✅ `stream` 필드 존재 여부 확인 후 처리
            if (!root.has("stream")) {
                logger.warn("⚠️ WebSocket 메시지에 'stream' 필드가 없음: {}", message);
                return;
            }

            String stream = root.get("stream").asText();
            JsonNode data = root.get("data");

            // ✅ `data` 필드 존재 여부 확인
            if (data == null) {
                logger.warn("⚠️ WebSocket 메시지에 'data' 필드가 없음: {}", message);
                return;
            }

            logger.debug("📩 WebSocket 메시지 수신 [{}]: {}", stream, data.toString());

            if (stream.contains("@kline")) {
                handleKlineMessage(data);
            } else if (stream.contains("@ticker")) {
                handleTickerMessage(data);
            } else if (stream.contains("@trade")) {
                handleTradeMessage(data);
            } else if (stream.contains("@aggTrade")) {
                handleAggTradeMessage(data);
            } else if (stream.contains("@markPrice")) {
                handleMarkPriceMessage(data);
            } else {
                logger.warn("⚠️ 알 수 없는 데이터 수신: {}", stream);
            }
        } catch (Exception e) {
            logger.error("❌ WebSocket 메시지 처리 오류: ", e);
        }
    }

    /**
     * ✅ Kline (캔들) 데이터 저장
     */
    private void handleKlineMessage(JsonNode data) {
        try {
            BinanceKlineDTO klineDTO = objectMapper.treeToValue(data, BinanceKlineDTO.class);
            if (klineDTO.getIsKlineClosed()) {
                klineService.saveKline(klineDTO);
                logger.debug("📊 Kline 저장됨: {}", klineDTO);
            }
        } catch (Exception e) {
            logger.error("❌ Kline 저장 오류: ", e);
        }
    }

    /**
     * ✅ Ticker (24시간 가격 변동) 데이터 저장
     */
    private void handleTickerMessage(JsonNode data) {
        try {
            BinanceTickerDTO tickerDTO = objectMapper.treeToValue(data, BinanceTickerDTO.class);
            tickerService.saveTicker(tickerDTO);
            logger.debug("📈 Ticker 저장됨: {}", tickerDTO);
        } catch (Exception e) {
            logger.error("❌ Ticker 저장 오류: ", e);
        }
    }

    /**
     * ✅ Trade (거래 체결 정보) 데이터 저장
     */
    private void handleTradeMessage(JsonNode data) {
        try {
            BinanceTradeDTO tradeDTO = objectMapper.treeToValue(data, BinanceTradeDTO.class);
            tradeService.saveTrade(tradeDTO);
            logger.debug("💹 Trade 저장됨: {}", tradeDTO);
        } catch (Exception e) {
            logger.error("❌ Trade 저장 오류: ", e);
        }
    }

    /**
     * ✅ Aggregate Trade (묶음 거래) 데이터 저장
     */
    private void handleAggTradeMessage(JsonNode data) {
        try {
            BinanceAggTradeDTO aggTradeDTO = objectMapper.treeToValue(data, BinanceAggTradeDTO.class);
            aggTradeService.saveAggTrade(aggTradeDTO);
            logger.debug("📦 Aggregate Trade 저장됨: {}", aggTradeDTO);
        } catch (Exception e) {
            logger.error("❌ Aggregate Trade 저장 오류: ", e);
        }
    }

    /**
     * ✅ Mark Price (시장 가격 및 펀딩 비율) 데이터 저장
     */
    private void handleMarkPriceMessage(JsonNode data) {
        try {
            BinanceFundingRateDTO fundingRateDTO = objectMapper.treeToValue(data, BinanceFundingRateDTO.class);
            fundingRateService.saveFundingRate(fundingRateDTO);
            logger.debug("🔄 Mark Price 저장됨: {}", fundingRateDTO);
        } catch (Exception e) {
            logger.error("❌ Mark Price 저장 오류: ", e);
        }
    }

    /**
     * ✅ WebSocket 연결 종료 시 재연결 처리
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.warn("❌ Binance WebSocket 연결 종료: {} {} {} ", code, reason, remote);
        reconnectWithDelay();
    }


    /**
     * ✅ WebSocket 오류 발생 시 재연결
     */
    @Override
    public void onError(Exception ex) {
        logger.error("❌ Binance WebSocket 오류 발생: ", ex);
        reconnectWithDelay();
    }


    private void reconnectWithDelay() {
        // 재연결 시도 횟수가 MAX_RECONNECT_ATTEMPTS 이하일 때만 실행
        while (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            // 지수적으로 대기 시간 증가, 최대 30초 (30000ms)로 제한
            long delay = Math.min(RECONNECT_DELAY * (long) Math.pow(2, reconnectAttempts), 30000);
            reconnectAttempts++;

            logger.debug("⏳ {}ms 후 WebSocket 재연결 시도 ({} / {})", delay, reconnectAttempts, MAX_RECONNECT_ATTEMPTS);

            try {
                Thread.sleep(delay);  // 대기 후 재연결 시도
                reconnect();  // 재연결 메서드 호출
                return; // 성공적으로 재연결 되었으면 종료
            } catch (InterruptedException e) {
                // 스레드 중단 예외 처리
                Thread.currentThread().interrupt();
                logger.error("❌ 재연결 중단됨: ", e);
                break; // InterruptedException 발생 시 루프 종료
            }
        }

        // 최대 재연결 시도 횟수를 초과한 경우 로그 남기기
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            logger.error("❌ 최대 재연결 시도 횟수 초과. WebSocket 연결 종료.");
        }
    }

    /**
     * ✅ WebSocket 현재 상태 확인
     */
    public boolean isWebSocketOpen() {
        return this.isOpen();
    }

    public boolean isWebSocketClosed() {
        return this.isClosed();
    }

}
