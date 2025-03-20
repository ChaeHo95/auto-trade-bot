package com.example.autotradebot.scheduler;

import com.example.autotradebot.dto.*;
import com.example.autotradebot.enums.TradePosition;
import com.example.autotradebot.exception.BinanceApiException;
import com.example.autotradebot.manager.TradeSignalCacheManager;
import com.example.autotradebot.mapper.UserPositionHistoryMapper;
import com.example.autotradebot.mapper.UserSettingMapper;
import com.example.autotradebot.mapper.UserTradeProcessMapper;
import com.example.autotradebot.mapper.VendorApiKeysMapper;
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
    private final ReentrantLock tradeLock = new ReentrantLock();
    private final ReentrantLock tradeCheckLock = new ReentrantLock();

    @Autowired
    private BinanceService binanceService;

    @Autowired
    private TradeSignalCacheManager tradeSignalCacheManager;

    @Autowired
    private OrderTradeService orderTradeService;

    @Autowired
    private UserSettingMapper userSettingMapper;

    @Autowired
    private UserPositionHistoryMapper userPositionHistoryMapper;

    @Autowired
    private VendorApiKeysMapper vendorApiKeysMapper;

    @Autowired
    private UserTradeProcessMapper userTradeProcessMapper;

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
        tradeLock.lock();
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

                        List<UserSettingDto> users = userSettingMapper.selectAllUserSettingsBySymbol(symbol);

                        users.parallelStream().forEach(user -> {
                            try {
                                String emailPk = user.getEmailPk();
                                UserTradeProcessDto userTradeProcessDto = userTradeProcessMapper.selectUserTradeProcessByEmailPkWithSymbol(emailPk, symbol);

                                if (userTradeProcessDto != null) {
                                    logger.info("Trade for symbol: {} is currently in progress for user: {}", symbol, emailPk);
                                    return;
                                }

                                logger.info("Trade Oder START for symbol: {}, user: {}", symbol, emailPk);
                                TradePosition signalPosition = tradeSignal.getPosition();

                                UserPositionHistoryDto previousPosition = userPositionHistoryMapper.selectUserLastPositionHistoryByEmailPk(emailPk);

                                VendorApiKeyDto vendorApiKeyDto = vendorApiKeysMapper.selectVendorApiKeyByEmailPk(emailPk);

                                if (signalPosition.equals(TradePosition.WAIT)) {
                                    logger.info("Signal position is 'WAIT' for symbol: {}. No action taken, exiting method.", symbol);
                                    return;
                                }

                                if (signalPosition.equals(TradePosition.EXIT) && (previousPosition == null || TradePosition.EXIT.equals(previousPosition.getPosition()))) {
                                    logger.info("Symbol: {} has no cached position, but signal position is 'EXIT'. Exiting method.", symbol);
                                    return;
                                }

                                if (stepSize == null) {
                                    logger.warn("Step size not found in cache for symbol: {}", symbol);
                                    return;
                                }

                                orderTradeService.trade(tradeSignal, user, vendorApiKeyDto, previousPosition, stepSize);

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
            tradeLock.unlock();
        }
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void processTradeOderCheck() {
        tradeCheckLock.lock();
        try {
            tradeSignalCacheManager.getAllTradeSignals()
                    .entrySet()
                    .parallelStream()
                    .forEach(entry -> {
                        String symbol = entry.getKey();

                        logger.info("Processing Trade Oder Check Symbol: {}", symbol);

                        List<UserTradeProcessDto> userTradeProcessDtos = userTradeProcessMapper.selectAllUserTradeProcesses(symbol);

                        userTradeProcessDtos.parallelStream().forEach(order -> {
                            try {
                                String emailPk = order.getEmailPk();
                                logger.info("Trade Oder START for symbol: {}, user: {}", symbol, emailPk);

                                VendorApiKeyDto vendorApiKeyDto = vendorApiKeysMapper.selectVendorApiKeyByEmailPk(emailPk);

                                orderTradeService.checkOrderStatus(order, vendorApiKeyDto);

                                logger.info("Trade Oder Check END for symbol: {}", symbol);
                            } catch (BinanceApiException e) {
                                logger.error(e.getMessage());
                            } catch (Exception e) {
                                logger.error("Trade Oder Check 도중 예외 발생");
                                logger.error(e.getMessage(), e);
                            }

                        });
                    });
        } finally {
            tradeCheckLock.unlock();
        }
    }
}
