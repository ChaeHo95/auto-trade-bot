package com.example.autotradebot.service.analysis;

import com.example.autotradebot.dto.analysis.*;
import com.example.autotradebot.dto.binance.BinanceLiquidationOrderDTO;
import com.example.autotradebot.dto.binance.BinanceOrderBookEntryDTO;
import com.example.autotradebot.dto.binance.BinancePartialBookDepthDTO;
import com.example.autotradebot.mapper.analysis.MarketAnalysisMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketAnalysisService {

    private final MarketAnalysisMapper marketAnalysisMapper;

    @Autowired
    public MarketAnalysisService(MarketAnalysisMapper marketAnalysisMapper) {
        this.marketAnalysisMapper = marketAnalysisMapper;
    }

    /**
     * ✅ AI 분석용 데이터 개별적으로 가져오기 및 설정
     */
    public MarketAnalysisDTO getMarketAnalysis(String symbol, String botType, int limit) {
        // 1️⃣ 심볼 설정
        MarketAnalysisDTO.MarketAnalysisDTOBuilder analysisBuilder = MarketAnalysisDTO.builder()
                .symbol(symbol);

        try {
            // 2️⃣ 최근 200개 캔들 데이터 가져오기 (BinanceKlineDTO → MarketAnalysisKlineDTO 변환)
            List<MarketAnalysisKlineDTO> recentKlines = marketAnalysisMapper.getRecentKlines(symbol, limit)
                    .stream()
                    .map(kline -> MarketAnalysisKlineDTO.builder()
                            .openTime(kline.getOpenTime())
                            .openPrice(kline.getOpenPrice())
                            .highPrice(kline.getHighPrice())
                            .lowPrice(kline.getLowPrice())
                            .closePrice(kline.getClosePrice())
                            .volume(kline.getVolume())
                            .closeTime(kline.getCloseTime())
                            .build())
                    .collect(Collectors.toList());
            analysisBuilder.recentKlines(recentKlines);
        } catch (Exception e) {
            System.err.println("recentKlines e = " + e);
        }

        try {
            // 3️⃣ 최근 100개 체결 거래 데이터 가져오기 (BinanceTradeDTO → MarketAnalysisTradeDTO 변환)
            List<MarketAnalysisTradeDTO> recentTrades = marketAnalysisMapper.getRecentTrades(symbol, limit)
                    .stream()
                    .map(trade -> MarketAnalysisTradeDTO.builder()
                            .price(trade.getPrice())
                            .quantity(trade.getQuantity())
                            .tradeTime(trade.getTradeTime())
                            .isBuyerMaker(trade.getBuyerMaker())
                            .build())
                    .collect(Collectors.toList());
            analysisBuilder.recentTrades(recentTrades);
        } catch (Exception e) {
            System.err.println("recentTrades e = " + e);
        }

        try {
            // 4️⃣ 최근 20개 펀딩 비율 데이터 가져오기 (BinanceFundingRateDTO → MarketAnalysisFundingRateDTO 변환)
            List<MarketAnalysisFundingRateDTO> fundingRates = marketAnalysisMapper.getFundingRates(symbol, limit)
                    .stream()
                    .map(fundingRate -> MarketAnalysisFundingRateDTO.builder()
                            .fundingTime(fundingRate.getFundingTime())
                            .fundingRate(fundingRate.getFundingRate())
                            .symbol(fundingRate.getSymbol())
                            .mark_price(fundingRate.getMarkPrice())
                            .build())
                    .collect(Collectors.toList());
            analysisBuilder.fundingRates(fundingRates);
        } catch (Exception e) {
            System.err.println("fundingRates e = " + e);
        }

        try {
            // 5️⃣ AI 분석 예측 정보 가져오기
            PredictionDTO chartAnalysis = marketAnalysisMapper.getChartAnalysis(symbol, botType);
            analysisBuilder.currentPosition(chartAnalysis);
        } catch (Exception e) {
            System.err.println("chartAnalysis e = " + e);
        }

        try {
            // 6️⃣ 이동 평균 값 가져오기
            BigDecimal movingAverage = marketAnalysisMapper.getMovingAverage(symbol, limit);
            analysisBuilder.movingAverage(movingAverage);
        } catch (Exception e) {
            System.err.println("movingAverage e = " + e);
        }

        try {
            // 7️⃣ RSI 값 가져오기
            BigDecimal rsiValue = marketAnalysisMapper.getRSIValue(symbol);
            analysisBuilder.rsiValue(rsiValue);
        } catch (Exception e) {
            System.err.println("rsiValue e = " + e);
        }

        try {
            // 8️⃣ MACD 값 가져오기
            BigDecimal macdValue = marketAnalysisMapper.getMACDValue(symbol);
            analysisBuilder.macdValue(macdValue);
        } catch (Exception e) {
            System.err.println("macdValue e = " + e);
        }

        try {
            // 9️⃣ 최근 청산 주문 데이터 가져오기 (BinanceLiquidationOrderDTO → MarketAnalysisLiquidationOrderDTO 변환)
            List<BinanceLiquidationOrderDTO> liquidationOrders = marketAnalysisMapper.getLiquidationOrders(symbol, limit);
            analysisBuilder.liquidationOrders(liquidationOrders);
        } catch (Exception e) {
            System.err.println("liquidationOrders e = " + e);
        }

        try {
            // 🔟 최근 부분 주문서 데이터 가져오기 (BinancePartialBookDepthDTO → MarketAnalysisPartialBookDepthDTO 변환)
            List<BinancePartialBookDepthDTO> partialBookDepth = marketAnalysisMapper.getPartialBookDepth(symbol, limit);

            // 2️⃣ 각 부분 주문서에 대해 주문서 항목 데이터 조회
            for (BinancePartialBookDepthDTO depth : partialBookDepth) {
                // 3️⃣ 해당 심볼과 event_time을 기준으로 주문서 항목 조회
                List<BinanceOrderBookEntryDTO> orderBookEntries = marketAnalysisMapper.getOrderBookEntriesBySymbolAndEventTime(symbol, depth.getEventTime());
                List<BinancePartialBookDepthDTO.OrderBookEntry> bids = new ArrayList();
                List<BinancePartialBookDepthDTO.OrderBookEntry> asks = new ArrayList();
                for (BinanceOrderBookEntryDTO entry : orderBookEntries) {
                    BinancePartialBookDepthDTO.OrderBookEntry orderBookEntry = new BinancePartialBookDepthDTO.OrderBookEntry(entry.getPrice(), entry.getQuantity());
                    if ("BID".equals(entry.getOrderType())) {

                        bids.add(orderBookEntry);
                    } else {
                        asks.add(orderBookEntry);
                    }
                }
                depth.setBids(bids);
                depth.setAsks(asks);
            }

            analysisBuilder.partialBookDepth(partialBookDepth);
        } catch (Exception e) {
            System.err.println("partialBookDepth e = " + e);
        }

        // ✅ 최종 DTO 빌드하여 반환
        return analysisBuilder.build();
    }
}
