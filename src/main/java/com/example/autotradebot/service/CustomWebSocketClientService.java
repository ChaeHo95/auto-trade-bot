package com.example.autotradebot.service;

import com.example.autotradebot.config.EnvConfig;
import com.example.autotradebot.dto.TradeSignalDto;
import com.example.autotradebot.manager.TradeSignalCacheManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CustomWebSocketClientService {

    private static final Logger logger = LoggerFactory.getLogger(CustomWebSocketClientService.class);
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long INITIAL_DELAY_MS = 60000L; // 초기 재연결 지연 시간 1분
    private static final long MAX_DELAY_MS = 60000L;    // 최대 재연결 지연 시간 1분
    private final ReentrantLock lock = new ReentrantLock();

    @Autowired
    private EnvConfig envConfig;

    @Autowired
    private TradeSignalCacheManager tradeSignalCacheManager;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private ThreadPoolTaskScheduler taskScheduler;
    private int reconnectAttempts = 0;

    /**
     * 애플리케이션 시작 시 TaskScheduler 초기화 후 STOMP 웹소켓 연결을 시도합니다.
     */
    @PostConstruct
    public void init() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("stomp-ws-");
        taskScheduler.initialize();

        // SockJS 클라이언트 설정
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        WebSocketClient sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        stompClient.setTaskScheduler(taskScheduler);
        stompClient.setDefaultHeartbeat(new long[]{60000, 60000});

        connect();
    }

    /**
     * STOMP 웹소켓 서버에 연결합니다.
     */
    public void connect() {
        String baseUrl = envConfig.getWebSocketUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            logger.error("❌ WEB_SOCKET_URL이 설정되지 않았습니다!");
            return;
        }
        String endpoint = baseUrl + "/ws";
        logger.info("STOMP 웹소켓 연결 시도: {}", endpoint);

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                logger.info("✅ STOMP 웹소켓 연결 성공: {}", session.getSessionId());
                reconnectAttempts = 0; // 연결 성공 시 재연결 시도 횟수 초기화

                // 예: /topic/positions 경로 구독
                session.subscribe("/topic/positions", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return TradeSignalDto.class; // 필요에 따라 DTO 타입 지정 가능
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        logger.info("📩 수신한 메시지: {}", payload);
                        TradeSignalDto tradeSignalDto = (TradeSignalDto) payload;
                        tradeSignalCacheManager.putTradeSignal(tradeSignalDto.getSymbol(), tradeSignalDto);
                        // 메시지 처리 로직 추가
                    }
                });
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                logger.error("❌ STOMP 전송 오류 발생: ", exception);
                scheduleReconnect();
            }
        };

        try {
            stompSession = stompClient.connectAsync(endpoint, sessionHandler).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("❌ STOMP 연결 실패: ", e);
            scheduleReconnect();
        }
    }

    /**
     * 주기적으로 STOMP 연결 상태를 확인하고, 연결이 끊겼으면 재연결을 시도합니다.
     * (필요에 따라 @Scheduled 어노테이션을 활성화하세요.)
     */
    @Scheduled(fixedRate = 60000)
    public void checkConnection() {
        lock.lock();
        try {
            if (stompSession == null || !stompSession.isConnected()) {
                logger.warn("⚠ STOMP 연결이 열려있지 않습니다. 재연결 시도합니다.");
                scheduleReconnect();
            } else {
                logger.info("✅ STOMP 연결 정상: {}", stompSession.getSessionId());
            }
        } finally {
            lock.unlock();
        }

    }

    /**
     * 재연결 시도를 예약합니다.
     */
    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            logger.error("❌ 최대 재연결 시도 {}회를 초과했습니다. 재연결을 중단합니다.", MAX_RECONNECT_ATTEMPTS);
            return;
        }
        reconnectAttempts++;
        long delay = Math.min(INITIAL_DELAY_MS * reconnectAttempts, MAX_DELAY_MS);
        logger.info("⚠️ {}회째 재연결 시도 예약 ({}ms 후)", reconnectAttempts, delay);
        taskScheduler.schedule(() -> {
            logger.info("🔄 재연결 시도 중 ({}회째)...", reconnectAttempts);
            connect();
        }, new Date(System.currentTimeMillis() + delay));
    }
}
