package com.example.autotradebot.service;

import com.example.autotradebot.dto.PositionDto;
import com.example.autotradebot.dto.TradeSignalDto;
import com.example.autotradebot.dto.UserSettingDto;
import com.example.autotradebot.exception.BinanceApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class OrderTradeService {

    private Logger logger = LoggerFactory.getLogger(OrderTradeService.class);


    @Autowired
    private BinanceService binanceService;


    public void trade(TradeSignalDto tradeSignal, UserSettingDto userSettingDto, PositionDto previousPosition, Integer stepSize) {

        try {
            String symbol = tradeSignal.getSymbol();
            String orderPosition = tradeSignal.getPosition();
            String accesskey = userSettingDto.getAccessKey();
            String secretKey = userSettingDto.getSecretKey();
            BigDecimal amount = userSettingDto.getAmount();

            logger.info("=== Trade 시작 ===");
            logger.info("심볼 [{}]에 대한 Trade 진행합니다.", symbol);

            String asset = "USDT";
            logger.info("잔고 조회 중: 자산 = {}", asset);
            BigDecimal balance = binanceService.getAvailableBalance(asset, accesskey, secretKey);
            logger.info("현재 잔고: {} {}", balance, asset);

            if (!"EXIT".equals(orderPosition) && balance.compareTo(amount) < 0) {
                logger.info("현재 잔고 부족: {} {}", balance, asset);
                return;
            }

            BigInteger leverage = BigInteger.ONE;
            BigInteger tradeLeverage = tradeSignal.getLeverage();

            if (tradeLeverage != null && !"EXIT".equals(orderPosition)) {
                logger.info("레버리지 설정 중: {} 배", tradeLeverage);
                binanceService.setLeverage(symbol, tradeLeverage, accesskey, secretKey).block();
                leverage = tradeLeverage;
                logger.info("레버리지 설정 완료");
            }

            int limit = 10;
            logger.info("호가 조회 중: 심볼 = {}, limit = {}", symbol, limit);
            Map<String, Object> orderBook = binanceService.getOrderBook(symbol, limit);
            logger.info("호가 응답: {}", orderBook);

            List<List<String>> bids = (List<List<String>>) orderBook.get("bids");
            List<List<String>> asks = (List<List<String>>) orderBook.get("asks");
            logger.info("매수 호가(Bids): {}", bids);
            logger.info("매도 호가(Asks): {}", asks);

            BigDecimal price = tradeSignal.getEntryPrice();

            boolean isClose = false;

            if (orderPosition.equals("LONG")) {
                BigDecimal current = new BigDecimal(bids.get(2).get(0));
                if (price.compareTo(current) > 0) {
                    price = current;
                }
            } else if (orderPosition.equals("SHORT")) {
                BigDecimal current = new BigDecimal(asks.get(2).get(0));
                if (price.compareTo(current) < 0) {
                    price = current;
                }
            } else if (orderPosition.equals("EXIT")) {
                if (previousPosition != null) {
                    String prevPosition = previousPosition.getPosition();

                    if (prevPosition.equals("LONG")) {
                        price = new BigDecimal(asks.get(2).get(0));
                    } else if (prevPosition.equals("SHORT")) {
                        price = new BigDecimal(bids.get(2).get(0));
                    }

                    isClose = true;
                }

            }

            if (price == null) {
                return;
            }
            /**
             * TODO emailPk , symbol 유저 이전 포지션 검색 필요
             * */
            BigDecimal feeRate = new BigDecimal("0.02");
            BigDecimal leverageValue = new BigDecimal(leverage);
            BigInteger orderId = null;
            BigDecimal quantity = BigDecimal.ZERO;

            if (isClose) {
                // 호가에서 가격 추출 (최우선 매도 호가 사용)
                logger.info("진입 주문 가격: {}", price);

                // 포지션 진입 주문
                quantity = previousPosition.getQuantity();
                orderId = binanceService.closeOrder(symbol, quantity, price, previousPosition.getPosition(), accesskey, secretKey);
            } else {
                // 수수료율, 레버리지, 포지션 크기, 잔고 등은 이미 계산되어 있다고 가정합니다.
                BigDecimal positionSize = amount.multiply(leverageValue); // 포지션 크기 계산
                BigDecimal fee = positionSize.multiply(feeRate);
                BigDecimal size = positionSize.subtract(fee);

                // 주문 가격(price)와 pricePrecision은 이미 계산되어 있다고 가정합니다.
                // 주문 수량을 계산 (포지션 크기 / 가격)
                quantity = size.divide(price, stepSize, BigDecimal.ROUND_DOWN);
                orderId = binanceService.openOrder(symbol, quantity, price, orderPosition, accesskey, secretKey);
            }

            logger.info("포지션 주문 요청 중: 심볼={}, 수량={}, 가격={}, 포지션 타입={}", symbol, quantity, price, orderPosition);

            if (orderId == null) {
                return;
            }

            logger.info("포지션 주문 응답: orderId={}", orderId);

            int maxRetries = 5;
            int retryCount = 0;
            while (retryCount < maxRetries) {
                try {

                    // 2초 대기 (주문 체결 대기)
                    logger.info("주문 체결 대기: 60초 대기");
                    Thread.sleep(60000);

                    // 주문 상태 확인
                    logger.info("주문 상태 조회 요청 중: 심볼={}, orderId={}", symbol, orderId);
                    String status = binanceService.orderStatus(symbol, orderId, accesskey, secretKey);
                    logger.info("주문 상태 응답: {}", status);

                    // 주문 체결되지 않은 경우 주문 취소 시도
                    if ("NEW".equals(status)) {
                        logger.info("주문 미체결 상태, 주문 취소 요청 진행: 심볼={}, orderId={}", symbol, orderId);
                        String cancel = binanceService.orderCancel(symbol, orderId, accesskey, secretKey);
                        logger.info("주문 취소 응답: {}", cancel);

                        if ("CANCELED".equals(cancel)) {
                            logger.info("주문 취소 완료");
                        } else {
                            logger.warn("주문 취소 실패 또는 다른 상태: {}", cancel);
                        }

                    } else {
                        logger.info("주문 체결 완료 - 추가 작업 없음");
                        PositionDto positionDto = new PositionDto();
                        positionDto.setSymbol(symbol);
                        positionDto.setLeverage(leverage);
                        positionDto.setQuantity(quantity);
                        positionDto.setEntryPrice(price);
                        positionDto.setPosition(orderPosition);

                        if (orderPosition.equals("EXIT")) {
                            String previousLeverageString = previousPosition.getLeverage().toString();
                            BigDecimal previousLeverage = new BigDecimal(previousLeverageString);
                            BigDecimal entryPrice = positionDto.getEntryPrice();

                            BigDecimal realBalance = quantity
                                    .multiply(BigDecimal.ONE.subtract(feeRate.multiply(BigDecimal.TWO)))
                                    .multiply(entryPrice)
                                    .divide(previousLeverage, 10, RoundingMode.HALF_DOWN);
                            /**
                             * TODO 유저 포지션 정리 시 값 업데이트 또는 히스토리 생성 필요
                             * */

                        } else {
                            /**
                             * TODO 유저 포지션 진입 시 값 업데이트 또는 히스토리 생성 필요
                             * */
                        }

                    }
                    break;
                } catch (BinanceApiException e) {
                    retryCount++;
                    logger.warn("BinanceApiException 발생, 재시도 {}/{} 진행", retryCount, maxRetries, e);
                    if (retryCount >= maxRetries) {
                        // 최대 재시도 횟수를 초과한 경우 BinanceApiException 던짐
                        throw e;
                    }
                } catch (Exception e) {
                    retryCount++;
                    logger.warn("일반 Exception 발생, 재시도 {}/{} 진행", retryCount, maxRetries, e);
                    if (retryCount >= maxRetries) {
                        // 최대 재시도 횟수를 초과한 경우 RuntimeException 던짐
                        throw new RuntimeException(e);
                    }
                }
            }

            logger.info("=== Trade 종료 ===");

        } catch (BinanceApiException e) {
            throw new BinanceApiException(e.getMessage());
        } catch (Exception e) {
            logger.error("TRADE  도중 예외 발생");
            throw new RuntimeException(e);
        }
    }
}



