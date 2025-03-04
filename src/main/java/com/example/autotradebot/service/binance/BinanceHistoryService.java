package com.example.autotradebot.service.binance;

import com.example.autotradebot.config.binance.BinanceConfig;
import com.example.autotradebot.dto.binance.*;
import com.example.autotradebot.mapper.binance.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class BinanceHistoryService {

    private final Logger logger = LoggerFactory.getLogger(BinanceHistoryService.class);
    private final BinanceKlineMapper klineMapper;
    private final BinanceTickerMapper tickerMapper;
    private final BinanceTradeMapper tradeMapper;
    private final BinanceFundingRateMapper fundingRateMapper;
    private final BinanceAggTradeMapper aggTradeMapper;
    private final String binanceApiUrl;

    @Autowired
    public BinanceHistoryService(BinanceConfig binanceConfig, BinanceKlineMapper klineMapper,
                                 BinanceTickerMapper tickerMapper, BinanceTradeMapper tradeMapper,
                                 BinanceFundingRateMapper fundingRateMapper, BinanceAggTradeMapper aggTradeMapper) {
        this.klineMapper = klineMapper;
        this.tickerMapper = tickerMapper;
        this.tradeMapper = tradeMapper;
        this.fundingRateMapper = fundingRateMapper;
        this.aggTradeMapper = aggTradeMapper;
        this.binanceApiUrl = binanceConfig.getBinanceApiUri();

    }

    /**
     * ✅ 선물 Continuous Kline 데이터 저장
     */
    public void saveHistoricalKlines(String market, String interval, BigInteger startTime) {
        logger.info("📡 {} 선물 Continuous Kline 데이터 요청 (interval: {}, 시작 시간: {})", market, interval, startTime);

        String pair = market.toUpperCase();  // BTCUSDT → BTC 변환
        String contractType = "PERPETUAL";  // 무기한 계약 사용

        String url = String.format("%s/continuousKlines?pair=%s&contractType=%s&interval=%s&startTime=%d&limit=1500",
                binanceApiUrl, pair.toUpperCase(), contractType, interval, startTime);

        fetchAndStoreKlineData(url, market);
    }

    /**
     * ✅ Kline 데이터를 가져와서 저장하는 헬퍼 메소드
     */
    private void fetchAndStoreKlineData(String url, String market) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getCode() == HttpStatus.SC_OK) {
                    String json = EntityUtils.toString(response.getEntity());
                    JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();

                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonArray klineData = jsonArray.get(i).getAsJsonArray();

                        BinanceKlineDTO klineDTO = new BinanceKlineDTO();
                        BinanceKlineDTO.KlineData data = new BinanceKlineDTO.KlineData();

                        data.setOpenTime(klineData.get(0).getAsBigInteger());
                        data.setCloseTime(klineData.get(6).getAsBigInteger());
                        data.setOpenPrice(klineData.get(1).getAsBigDecimal());
                        data.setHighPrice(klineData.get(2).getAsBigDecimal());
                        data.setLowPrice(klineData.get(3).getAsBigDecimal());
                        data.setClosePrice(klineData.get(4).getAsBigDecimal());
                        data.setVolume(klineData.get(5).getAsBigDecimal());
                        data.setTradeCount(klineData.get(8).getAsBigInteger());

                        klineDTO.setSymbol(market.toUpperCase());
                        klineDTO.setKline(data);

                        klineMapper.insertKline(klineDTO);
                    }
                    logger.info("✅ {} Kline 데이터 저장 완료", market);
                }
            }
        } catch (Exception e) {
            logger.error("❌ {} Kline 데이터 저장 오류", market, e);
        }
    }

    /**
     * ✅ 24시간 선물 Ticker 데이터 저장
     */
    public void saveTicker(String market) {
        logger.info("📡 {} Ticker 데이터 요청 시작", market);

        String url = String.format("%s/ticker/24hr?symbol=%s", binanceApiUrl, market.toUpperCase());

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getCode() == HttpStatus.SC_OK) {
                    String json = EntityUtils.toString(response.getEntity());
                    JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

                    BinanceTickerDTO tickerDTO = new BinanceTickerDTO();
                    tickerDTO.setSymbol(jsonObject.get("symbol").getAsString());
                    tickerDTO.setPriceChange(jsonObject.get("priceChange").getAsBigDecimal());
                    tickerDTO.setPriceChangePercent(jsonObject.get("priceChangePercent").getAsBigDecimal());
                    tickerDTO.setWeightedAvgPrice(jsonObject.get("weightedAvgPrice").getAsBigDecimal());
                    tickerDTO.setLastPrice(jsonObject.get("lastPrice").getAsBigDecimal());
                    tickerDTO.setOpenPrice(jsonObject.get("openPrice").getAsBigDecimal());
                    tickerDTO.setHighPrice(jsonObject.get("highPrice").getAsBigDecimal());
                    tickerDTO.setLowPrice(jsonObject.get("lowPrice").getAsBigDecimal());
                    tickerDTO.setVolume(jsonObject.get("volume").getAsBigDecimal());
                    tickerDTO.setEventTime(jsonObject.get("closeTime").getAsBigInteger());

                    tickerMapper.insertTicker(tickerDTO);
                    logger.info("✅ {} Ticker 데이터 저장 완료", market);
                } else {
                    logger.warn("⚠️ {} Ticker 데이터 요청 실패 (응답 코드: {})", market, response.getCode());
                }
            }
        } catch (Exception e) {
            logger.error("❌ {} Ticker 데이터 저장 오류", market, e);
        }
    }

    /**
     * ✅ 최근 거래 내역 저장 (최대 1000개 요청 가능)
     */
    public void saveHistoricalTrades(String market, int limit) {
        logger.info("📡 {} Trade 데이터 요청 시작 (limit: {})", market, limit);

        String url = String.format("%s/trades?symbol=%s&limit=%d",
                binanceApiUrl, market.toUpperCase(), limit);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getCode() == HttpStatus.SC_OK) {
                    String json = EntityUtils.toString(response.getEntity());
                    JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();

                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();

                        BinanceTradeDTO tradeDTO = new BinanceTradeDTO();
                        tradeDTO.setTradeId(jsonObject.get("id").getAsBigInteger());
                        tradeDTO.setSymbol(market.toUpperCase());
                        tradeDTO.setPrice(jsonObject.get("price").getAsBigDecimal());
                        tradeDTO.setQuantity(jsonObject.get("qty").getAsBigDecimal());
                        tradeDTO.setTradeTime(jsonObject.get("time").getAsBigInteger());
                        tradeDTO.setBuyerMaker(jsonObject.get("isBuyerMaker").getAsBoolean());

                        tradeMapper.insertTrade(tradeDTO);
                    }
                    logger.info("✅ {} Trade 데이터 저장 완료", market);
                } else {
                    logger.warn("⚠️ {} Trade 데이터 요청 실패 (응답 코드: {})", market, response.getCode());
                }
            }
        } catch (Exception e) {
            logger.error("❌ {} Trade 데이터 저장 오류", market, e);
        }
    }

    /**
     * ✅ 펀딩 비율 데이터 저장
     */
    public void saveFundingRates(String market) {
        logger.info("📡 {} Funding Rate 데이터 요청 시작", market);

        String url = String.format("%s/fundingRate?symbol=%s&limit=1000", binanceApiUrl, market.toUpperCase());

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getCode() == HttpStatus.SC_OK) {
                    String json = EntityUtils.toString(response.getEntity());
                    JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();

                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();

                        BinanceFundingRateDTO fundingRateDTO = new BinanceFundingRateDTO();
                        fundingRateDTO.setSymbol(jsonObject.get("symbol").getAsString());
                        fundingRateDTO.setFundingRate(jsonObject.get("fundingRate").getAsBigDecimal());
                        fundingRateDTO.setFundingTime(jsonObject.get("fundingTime").getAsBigInteger());
                        fundingRateDTO.setMarkPrice(jsonObject.get("markPrice").getAsBigDecimal());

                        fundingRateMapper.insertFundingRate(fundingRateDTO);
                    }
                    logger.info("✅ {} Funding Rate 데이터 저장 완료", market);
                } else {
                    logger.warn("⚠️ {} Funding Rate 데이터 요청 실패 (응답 코드: {})", market, response.getCode());
                }
            }
        } catch (Exception e) {
            logger.error("❌ {} Funding Rate 데이터 저장 오류", market, e);
        }
    }

    /**
     * ✅ 누락된 펀딩 비율 데이터를 확인하고 가져오는 메소드
     */
    public void checkAndFetchMissingFundingData(String market) {
        BigInteger now = BigInteger.valueOf(Instant.now().toEpochMilli());

        BinanceFundingRateDTO latestFundingRate = fundingRateMapper.getLatestFundingRate(market.toUpperCase());
        BigInteger lastFundingTime = Optional.ofNullable(latestFundingRate)
                .map(BinanceFundingRateDTO::getFundingTime)
                .orElse(null);

        if (lastFundingTime == null || now.compareTo(lastFundingTime) > 0) {
            logger.warn("📥 {} Funding Rate 데이터 부족! 새로 가져오기", market);
            saveFundingRates(market);
            logger.info("✅ {} Funding Rate 데이터 충분 (최근 데이터: {})", market, lastFundingTime);
        } else {
            logger.info("✅ {} Funding Rate 데이터 최신 상태 유지 (최근 데이터: {})", market, lastFundingTime);
        }
    }

    /**
     * ✅ 기존 checkAndFetchMissingData 메소드에서 Funding Rate 추가
     */
    public void checkAndFetchMissingData(String market) {
        boolean isKlineUpdated = checkAndFetchMissingKlineData(market);
        if (isKlineUpdated) {
            checkAndFetchMissingTickerData(market);
            checkAndFetchMissingTradeData(market);
            checkAndFetchMissingFundingData(market);
            checkAndFetchMissingAggTradeData(market);
        }

    }

    private boolean checkAndFetchMissingKlineData(String market) {
        boolean isKlineUpdated = false;
        BigInteger now = BigInteger.valueOf(Instant.now().toEpochMilli());
        BigInteger oneHourAgo = now.subtract(BigInteger.valueOf(60 * 60 * 1000));

        BinanceKlineDTO latestKline = klineMapper.getLatestKline(market.toUpperCase());
        BigInteger lastKlineTime = Optional.ofNullable(latestKline)
                .map(BinanceKlineDTO::getOpenTime)
                .orElse(null);

        BigInteger start = lastKlineTime == null ? oneHourAgo : lastKlineTime;

        if (now.compareTo(start) > 0) {
            if (oneHourAgo.compareTo(start) > 0) {
                start = oneHourAgo;
            }
            logger.warn("📥 {} Kline 데이터 부족! {}부터 데이터 가져오기", market, start);
            saveHistoricalKlines(market, "1m", start);
            isKlineUpdated = true;
        } else {
            logger.info("✅ {} Kline 데이터 충분 (최근 데이터: {})", market, lastKlineTime);
        }
        return isKlineUpdated;
    }

    private void checkAndFetchMissingTickerData(String market) {
        BinanceTickerDTO latestTicker = tickerMapper.getLatestTicker(market.toUpperCase());
        BigInteger lastTickerTime = Optional.ofNullable(latestTicker)
                .map(BinanceTickerDTO::getEventTime)
                .orElse(null);

        logger.warn("📥 {} Ticker 데이터 부족! 새로 가져오기", market);
        saveTicker(market);
        logger.info("✅ {} Ticker 데이터 충분 (최근 데이터: {})", market, lastTickerTime);
    }

    private void checkAndFetchMissingTradeData(String market) {
        List<BinanceTradeDTO> latestTrades = tradeMapper.getLatestTrades(market.toUpperCase());
        BigInteger lastTradeTime = latestTrades.isEmpty() ? null : latestTrades.get(0).getTradeTime();

        logger.warn("📥 {} Trade 데이터 부족! 새로 가져오기", market);
        saveHistoricalTrades(market, 1000);
        logger.info("✅ {} Trade 데이터 충분 (최근 데이터: {})", market, lastTradeTime);
    }


    /**
     * ✅ Aggregate Trade 데이터 저장 (묶음 거래)
     */
    public void saveAggTrades(String market, int limit) {
        logger.info("📡 {} Aggregate Trade 데이터 요청 시작 (limit: {})", market, limit);

        String url = String.format("%s/aggTrades?symbol=%s&limit=%d",
                binanceApiUrl, market.toUpperCase(), limit);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getCode() == HttpStatus.SC_OK) {
                    String json = EntityUtils.toString(response.getEntity());
                    JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();

                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();

                        BinanceAggTradeDTO aggTradeDTO = new BinanceAggTradeDTO();
                        aggTradeDTO.setAggTradeId(jsonObject.get("a").getAsBigInteger());
                        aggTradeDTO.setSymbol(market.toUpperCase());
                        aggTradeDTO.setPrice(jsonObject.get("p").getAsBigDecimal());
                        aggTradeDTO.setQuantity(jsonObject.get("q").getAsBigDecimal());
                        aggTradeDTO.setTradeTime(jsonObject.get("T").getAsBigInteger());
                        aggTradeDTO.setBuyerMaker(jsonObject.get("m").getAsBoolean());

                        aggTradeMapper.insertAggTrade(aggTradeDTO);
                    }
                    logger.info("✅ {} Aggregate Trade 데이터 저장 완료", market);
                } else {
                    logger.warn("⚠️ {} Aggregate Trade 데이터 요청 실패 (응답 코드: {})", market, response.getCode());
                }
            }
        } catch (Exception e) {
            logger.error("❌ {} Aggregate Trade 데이터 저장 오류", market, e);
        }
    }

    /**
     * ✅ 누락된 Aggregate Trade 데이터 체크 후 자동 업데이트
     */
    public void checkAndFetchMissingAggTradeData(String market) {
        BigInteger now = BigInteger.valueOf(Instant.now().toEpochMilli());

        BinanceAggTradeDTO latestAggTrade = aggTradeMapper.getLatestAggTrade(market.toUpperCase());
        BigInteger lastAggTradeTime = Optional.ofNullable(latestAggTrade)
                .map(BinanceAggTradeDTO::getTradeTime)
                .orElse(null);

        if (lastAggTradeTime == null || now.compareTo(lastAggTradeTime) > 0) {
            logger.warn("📥 {} Aggregate Trade 데이터 부족! 새로 가져오기", market);
            saveAggTrades(market, 1000);
            logger.info("✅ {} Aggregate Trade 데이터 충분 (최근 데이터: {})", market, lastAggTradeTime);
        } else {
            logger.info("✅ {} Aggregate Trade 데이터 최신 상태 유지 (최근 데이터: {})", market, lastAggTradeTime);
        }
    }
}
