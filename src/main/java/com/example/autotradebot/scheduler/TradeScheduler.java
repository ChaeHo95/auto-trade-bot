package com.example.autotradebot.scheduler;

import com.example.autotradebot.dto.PositionDto;
import com.example.autotradebot.dto.TradeSignalDto;
import com.example.autotradebot.dto.UserSettingDto;
import com.example.autotradebot.exception.BinanceApiException;
import com.example.autotradebot.manager.TradeSignalCacheManager;
import com.example.autotradebot.service.BinanceService;
import com.example.autotradebot.service.OrderTradeService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "scheduling.trade", havingValue = "true", matchIfMissing = false)
public class TradeScheduler {
    private Logger logger = LoggerFactory.getLogger(TradeScheduler.class);
    private final ConcurrentHashMap<String, Integer> symbolStepSizeCache = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    @Autowired
    private BinanceService binanceService;

    @Autowired
    private TradeSignalCacheManager tradeSignalCacheManager;

    @Autowired
    private OrderTradeService orderTradeService;

    @PostConstruct
    public void init() {
        try {
            logger.info("TradeScheduler 초기화 시작.");
            Map<String, Integer> symbolStepSizes = binanceService.parseExchangeInfoForSymbols();
            symbolStepSizeCache.putAll(symbolStepSizes);
            logger.info("TradeScheduler 초기화 성공.");
        } catch (Exception e) {
            logger.error("TradeScheduler 초기화 중 오류 발생: ", e);
        }
    }


    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void processTradeOder() {
        lock.lock();
        try {
            tradeSignalCacheManager.getAllTradeSignals()
                    .entrySet()
                    .parallelStream()
                    .forEach(entry -> {
                        String symbol = entry.getKey();
                        TradeSignalDto tradeSignal = entry.getValue();
                        Integer stepSize = symbolStepSizeCache.get(symbol);

                        if (tradeSignal == null) {
                            return;
                        }

                        logger.info("Processing symbol: {}", symbol);

                        List<UserSettingDto> users = new ArrayList<>();
                        users.parallelStream().forEach(user -> {
                            try {
                                logger.info("Trade Oder START for symbol: {}, user: {}", symbol, user.getEmailPk());
                                String signalPosition = tradeSignal.getPosition();

                                /**
                                 * TODO emailPk , symbol 유저 포지션 검색 필요
                                 * */
                                PositionDto previousPosition = new PositionDto();
                                if (signalPosition.equals("WAIT")) {
                                    logger.info("Signal position is 'WAIT' for symbol: {}. No action taken, exiting method.", symbol);
                                    return;
                                }

                                if (signalPosition.equals("EXIT") && previousPosition == null) {
                                    logger.info("Symbol: {} has no cached position, but signal position is 'EXIT'. Exiting method.", symbol);
                                    return;
                                }

                                if (stepSize == null) {
                                    logger.warn("Step size not found in cache for symbol: {}", symbol);
                                    return;
                                }

                                orderTradeService.trade(tradeSignal, user, previousPosition, stepSize);

                                logger.info("Trade Oder END for symbol: {}", symbol);
                            } catch (BinanceApiException e) {
                                logger.error(e.getMessage());
                            } catch (Exception e) {
                                logger.error("TRADE ORDER 도중 예외 발생");
                                logger.error(e.getMessage(), e);
                            }

                        });
                    });
        } finally {
            lock.unlock();
        }
    }
}
