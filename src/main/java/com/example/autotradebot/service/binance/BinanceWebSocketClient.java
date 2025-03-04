package com.example.autotradebot.service.binance;

import com.example.autotradebot.dto.binance.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
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
    private Timer pingTimer;

    // ✅ 재연결 관련 변수
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long RECONNECT_DELAY = TimeUnit.SECONDS.toMillis(5);
    private int reconnectAttempts = 0;

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
     * ✅ WebSocket 연결 성공 시 호출
     */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("✅ Binance WebSocket 연결 성공!");
        reconnectAttempts = 0;
        startPing();
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

            logger.info("📩 WebSocket 메시지 수신 [{}]: {}", stream, data.toString());

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
                logger.info("📊 Kline 저장됨: {}", klineDTO);
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
            logger.info("📈 Ticker 저장됨: {}", tickerDTO);
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
            logger.info("💹 Trade 저장됨: {}", tradeDTO);
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
            logger.info("📦 Aggregate Trade 저장됨: {}", aggTradeDTO);
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
            logger.info("🔄 Mark Price 저장됨: {}", fundingRateDTO);
        } catch (Exception e) {
            logger.error("❌ Mark Price 저장 오류: ", e);
        }
    }


    /**
     * ✅ WebSocket 오류 발생 시 재연결 시도
     */
    @Override
    public void onError(Exception ex) {
        logger.error("❌ Binance WebSocket 오류 발생: ", ex);
        // Ping 전송 오류가 발생하면 바로 재연결 시도하도록 추가
        reconnectWithDelay();
    }


    /**
     * ✅ WebSocket Ping-Pong 유지 (3분마다 Ping 전송)
     */
    private void startPing() {
        // 기존 타이머 취소
        if (pingTimer != null) {
            pingTimer.cancel();
        }

        // 새로운 타이머 시작
        pingTimer = new Timer(true);
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendPing();
            }
        }, 0, 3 * 60 * 1000); // 3분마다 Ping 전송
    }

    /**
     * ✅ Binance WebSocket Ping 전송 (잘못된 JSON 방식 X)
     */
    @Override
    public void sendPing() {
        try {
            if (this.getConnection() != null && this.getConnection().isOpen()) {
                this.getConnection().sendPing();
                logger.info("📡 Ping 프레임 전송");
            } else {
                logger.error("❌ WebSocket 연결이 열려 있지 않습니다. Ping 전송 실패");
            }
        } catch (Exception e) {
            logger.error("❌ Ping 전송 실패: ", e);
        }
    }


    /**
     * ✅ WebSocket 연결 종료 시 처리
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.warn("❌ Binance WebSocket 연결 종료: {} {}", code, reason);
        if (pingTimer != null) {
            pingTimer.cancel();
        }

        // ✅ WebSocket 재연결을 별도 스레드에서 실행
        if (remote) {
            new Thread(this::reconnectWithDelay).start();
        }
    }

    /**
     * ✅ WebSocket 재연결 (별도 스레드에서 실행)
     */
    private void reconnectWithDelay() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            long delay = RECONNECT_DELAY * (long) Math.pow(2, reconnectAttempts);
            reconnectAttempts++;
            logger.info("⏳ {}ms 후 WebSocket 재연결 시도 ({} / {})", delay, reconnectAttempts, MAX_RECONNECT_ATTEMPTS);

            try {
                Thread.sleep(delay);
                reconnect(); // WebSocket 재연결
                startPing(); // 재연결 후 Ping 전송 시작
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("❌ 재연결 중단됨: ", e);
            }
        } else {
            logger.error("❌ 최대 재연결 시도 횟수 초과. WebSocket 연결 종료.");
        }
    }
}
