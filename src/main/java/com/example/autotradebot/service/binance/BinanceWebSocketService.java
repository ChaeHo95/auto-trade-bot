package com.example.autotradebot.service.binance;

import com.example.autotradebot.config.binance.BinanceConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@Service
public class BinanceWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceWebSocketService.class);

    private BinanceConfig binanceConfig;
    private BinanceKlineService binanceKlineService;
    private BinanceTickerService binanceTickerService;
    private BinanceTradeService binanceTradeService;
    private BinanceHistoryService binanceHistoryService;
    private BinanceAggTradeService binanceAggTradeService;
    private BinanceFundingRateService binanceFundingRateService;
    private final BinanceLiquidationOrderService liquidationOrderService;
    private final BinancePartialBookDepthService partialBookDepthService;

    private ObjectMapper objectMapper;
    private BinanceWebSocketClient webSocketClient;

    @Value("${symbols}")
    private List<String> symbols;

    @Value("${enable.binance.websocket:false}") // 기본값 false
    private boolean enableWebSocket;

    @Autowired
    public BinanceWebSocketService(
            BinanceConfig binanceConfig,
            BinanceKlineService binanceKlineService,
            BinanceTickerService binanceTickerService,
            BinanceTradeService binanceTradeService,
            BinanceHistoryService binanceHistoryService,
            BinanceAggTradeService binanceAggTradeService,
            BinanceFundingRateService binanceFundingRateService,
            BinanceLiquidationOrderService liquidationOrderService,
            BinancePartialBookDepthService partialBookDepthService,
            ObjectMapper objectMapper
    ) {
        this.binanceConfig = binanceConfig;
        this.binanceKlineService = binanceKlineService;
        this.binanceTickerService = binanceTickerService;
        this.binanceTradeService = binanceTradeService;
        this.binanceHistoryService = binanceHistoryService;
        this.binanceAggTradeService = binanceAggTradeService;
        this.binanceFundingRateService = binanceFundingRateService;
        this.liquidationOrderService = liquidationOrderService;
        this.partialBookDepthService = partialBookDepthService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initWebSocket() {
        if (!enableWebSocket) {
            logger.info("⚠ WebSocket 실행이 비활성화됨.");
            return; // 실행하지 않음
        }

        try {
            // ✅ 구독할 심볼 리스트 설정
            List<String> markets = symbols.stream()
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .toList(); // 필요한 심볼 추가 가능

            // ✅ WebSocket 시작 전 최신 데이터 확인 & 보충
            for (String market : markets) {
                logger.info("🔍 {} 심볼의 누락 데이터 확인 및 보충 시작...", market);
                binanceHistoryService.checkAndFetchMissingData(market);
            }

            // ✅ 동적으로 WebSocket URL 생성
            String webSocketUrl = binanceConfig.getFuturesWebSocketUrl(markets);

            // ✅ WebSocket 클라이언트 생성 및 연결
            URI webSocketUri = new URI(webSocketUrl);
            webSocketClient = new BinanceWebSocketClient(
                    webSocketUri, binanceKlineService, binanceTickerService,
                    binanceTradeService, binanceFundingRateService, binanceAggTradeService,
                    liquidationOrderService, partialBookDepthService, objectMapper
            );
            webSocketClient.connect();

            logger.info("✅ Binance WebSocket 연결 성공!");
        } catch (Exception e) {
            throw new RuntimeException("❌ WebSocket 초기화 실패: ", e);
        }
    }
}
