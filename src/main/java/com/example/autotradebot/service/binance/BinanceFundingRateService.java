package com.example.autotradebot.service.binance;

import com.example.autotradebot.dto.binance.BinanceFundingRateDTO;
import com.example.autotradebot.mapper.binance.BinanceFundingRateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;

@Service
public class BinanceFundingRateService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceFundingRateService.class);
    private final BinanceFundingRateMapper fundingRateMapper;

    public BinanceFundingRateService(BinanceFundingRateMapper fundingRateMapper) {
        this.fundingRateMapper = fundingRateMapper;
    }

    /**
     * ✅ 펀딩 비율 데이터 저장
     */
    public void saveFundingRate(BinanceFundingRateDTO fundingRateDTO) {
        try {
            fundingRateMapper.insertFundingRate(fundingRateDTO);
            logger.info("📊 펀딩 비율 저장됨: {}", fundingRateDTO);
        } catch (Exception e) {
            logger.error("❌ 펀딩 비율 저장 오류: ", e);
        }
    }

    /**
     * ✅ 특정 심볼의 최신 펀딩 비율 조회
     */
    public BinanceFundingRateDTO getLatestFundingRate(String symbol) {
        return fundingRateMapper.getLatestFundingRate(symbol);
    }

    /**
     * ✅ 특정 기간 동안의 펀딩 비율 조회
     */
    public List<BinanceFundingRateDTO> getFundingRatesInRange(String symbol, BigInteger startTime, BigInteger endTime) {
        return fundingRateMapper.getFundingRatesInRange(symbol, startTime.longValue(), endTime.longValue());
    }
}
