package com.example.autotradebot.service;

import com.example.autotradebot.config.BinanceConfig;
import com.example.autotradebot.enums.TradePosition;
import com.example.autotradebot.exception.BinanceApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BinanceService {

    private Logger logger = LoggerFactory.getLogger(BinanceService.class);

    private final WebClient webClient;

    @Autowired
    public BinanceService(BinanceConfig binanceConfig) {
        this.webClient = binanceConfig.binanceApiClient();
    }

    /**
     * ✅ 내 잔고 조회 (예: USDT)
     *
     * @param asset 조회할 자산 (예: "USDT")
     * @return 사용 가능한 잔고 (BigDecimal)
     */
    public BigDecimal getAvailableBalance(String asset, String accesskey, String secretKey) {
        String endpoint = "/fapi/v2/account";
        long timestamp = getBinanceServerTime();

        String queryString = "timestamp=" + timestamp;
        String signature = generateSignature(queryString, secretKey);

        Map<String, Object> result = webClient
                .get()
                .uri(uriBuilder -> uriBuilder.path(endpoint)
                        .query(queryString + "&signature=" + signature)
                        .build())
                .headers(httpHeaders -> {
                    httpHeaders.add("X-MBX-APIKEY", accesskey);
                })
                .retrieve()
                .onStatus(status -> status.value() == 400, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    return Mono.error(new BinanceApiException("Binance API Bad Request (400): " + errorBody));
                                })
                )
                .bodyToMono(Map.class)
                .block(); // 동기 호출

        if (result == null || !result.containsKey("assets")) {
            throw new RuntimeException("잔고 조회 실패: assets 정보 없음");
        }

        List<Map<String, Object>> assets = (List<Map<String, Object>>) result.get("assets");
        for (Map<String, Object> assetData : assets) {
            if (asset.equals(assetData.get("asset"))) {
                return new BigDecimal(assetData.get("availableBalance").toString());
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * ✅ 레버리지 설정 (지정가 주문 시에도 사용)
     *
     * @param symbol   거래 심볼 (예: "BTCUSDT")
     * @param leverage 설정할 레버리지 (1 ~ 125)
     * @return 설정 결과 응답 (Mono<String>)
     */
    public Mono<String> setLeverage(String symbol, BigInteger leverage, String accesskey, String secretKey) {
        String endpoint = "/fapi/v1/leverage";
        long timestamp = getBinanceServerTime();

        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("leverage", leverage);
        params.put("timestamp", timestamp);

        String queryString = generateQueryString(params);
        String signature = generateSignature(queryString, secretKey);
        logger.info(endpoint + queryString + "&signature=" + signature);
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path(endpoint)
                        .query(queryString + "&signature=" + signature)
                        .build())
                .headers(httpHeaders -> {
                    httpHeaders.add("X-MBX-APIKEY", accesskey);
                })
                .retrieve()
                .onStatus(status -> status.value() == 400, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    return Mono.error(new BinanceApiException("Binance API Bad Request (400): " + errorBody));
                                })
                )
                .bodyToMono(Map.class)
                .map(response -> response.toString())
                .doOnSuccess(resp -> logger.info("✅ 레버리지 설정 성공: {}", resp))
                .doOnError(error -> logger.error("❌ 레버리지 설정 실패: {}", error.getMessage()));
    }

    /**
     * ✅ 호가(Order Book) 조회
     *
     * @param symbol 거래 심볼 (예: "BTCUSDT")
     * @param limit  가져올 호가 개수 (예: 5, 10, 20 등)
     * @return 호가 데이터 (Map, keys: "bids", "asks")
     */
    public Map<String, Object> getOrderBook(String symbol, int limit) {
        String endpoint = "/fapi/v1/depth";

        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("limit", limit);

        String queryString = generateQueryString(params);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path(endpoint)
                        .query(queryString)
                        .build())
                .retrieve()
                .onStatus(status -> status.value() == 400, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    logger.error("Binance API 400 에러 발생: {}", errorBody);
                                    return Mono.error(new BinanceApiException("Binance API Bad Request (400): " + errorBody));
                                })
                )
                .bodyToMono(Map.class)
                .doOnSuccess(response -> logger.info("✅ 호가 조회 성공: {}", response))
                .doOnError(error -> logger.error("❌ 호가 조회 실패: {}", error.getMessage()))
                .block();
    }

    // 주문 진입
    public BigInteger openOrder(String symbol, BigDecimal quantity, BigDecimal price, TradePosition position, String accesskey, String secretKey) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("quantity", quantity);
        params.put("price", price);
        params.put("reduceOnly", "false");

        if (TradePosition.LONG.equals(position)) {
            params.put("side", "BUY");
        } else if (TradePosition.SHORT.equals(position)) {
            params.put("side", "SELL");
        } else {
            return null;
        }

        Map<String, Object> response = newOrder(params, accesskey, secretKey).block();

        if (response == null) {
            return null;
        }
        BigInteger orderId = new BigInteger(response.get("orderId").toString());

        return orderId;
    }

    // 주문 종료
    public BigInteger closeOrder(String symbol, BigDecimal quantity, BigDecimal price, TradePosition position, String accesskey, String secretKey) {
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);
        params.put("quantity", quantity);
        params.put("price", price);
        params.put("reduceOnly", "true");

        if (TradePosition.LONG.equals(position)) {
            params.put("side", "SELL");
        } else if (TradePosition.SHORT.equals(position)) {
            params.put("side", "BUY");
        } else {
            return null;
        }

        Map<String, Object> response = newOrder(params, accesskey, secretKey).block();

        if (response == null) {
            return null;
        }
        BigInteger orderId = new BigInteger(response.get("orderId").toString());

        return orderId;
    }

    /**
     * New Order(TRADE) 주문 요청 메서드 (요청 본문 방식)
     * <p>
     * 주문에 필요한 모든 파라미터를 Map<String, String>으로 전달합니다.
     * 필수 파라미터: symbol, side, type, timestamp 등이며,
     * LIMIT 주문인 경우 timeInForce, quantity, price 등이 추가로 필요합니다.
     * recvWindow가 전달되지 않으면 기본값으로 2000 (2초)을 적용합니다.
     * <p>
     * 모든 파라미터는 application/x-www-form-urlencoded 형식으로 본문에 담아 전송됩니다.
     *
     * @param params 주문 파라미터 Map
     * @return 주문 응답 결과 (Mono<Map<String, Object>>)
     */
    public Mono<Map> newOrder(Map<String, Object> params, String accesskey, String secretKey) {
        String endpoint = "/fapi/v1/order";
        long timestamp = getBinanceServerTime();
        params.put("timestamp", timestamp);
        params.put("type", "LIMIT");
        params.put("positionSide", "BOTH");
        params.put("timeInForce", "GTC");

        // 주문 파라미터를 key=value 형식의 쿼리 스트링으로 생성
        String queryString = generateQueryString(params);
        // HMAC SHA256 서명 생성 (formData 전체를 대상으로)
        String signature = generateSignature(queryString, secretKey);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path(endpoint)
                        .query(queryString + "&signature=" + signature)
                        .build())
                .headers(httpHeaders -> {
                    httpHeaders.add("X-MBX-APIKEY", accesskey);
                })
                .retrieve()
                .onStatus(status -> status.value() == 400, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    logger.error("Binance API 400 에러 발생: {}", errorBody);
                                    return Mono.error(new BinanceApiException("Binance API Bad Request (400): " + errorBody));
                                })
                )
                .bodyToMono(Map.class)
                .doOnSuccess(response -> logger.info("✅ New Order 성공: {}", response))
                .doOnError(error -> logger.error("❌ New Order 실패: {}", error.getMessage()));
    }

    /**
     * ✅ 주문 취소 메서드
     * <p>
     * Binance 선물 주문 취소 API (DELETE /fapi/v1/order)를 호출하여 활성 주문을 취소합니다.
     * 필수 파라미터: symbol, orderId 필요합니다.
     * recvWindow가 전달되지 않으면 기본값으로 2000 (2초)을 적용합니다.
     *
     * @param symbol 취소할 주문의 심볼 (예: "BTCUSDT")
     * @return 주문 취소 응답 결과 (Mono<Map<String, Object>>)
     */
    public String orderCancel(String symbol, BigInteger orderId, String accesskey, String secretKey) {
        String endpoint = "/fapi/v1/order";
        Map<String, Object> params = new HashMap<>();
        long timestamp = getBinanceServerTime();
        params.put("timestamp", timestamp);
        params.put("symbol", symbol);
        params.put("orderId", orderId);

        // 주문 파라미터를 key=value 형식의 쿼리 스트링으로 생성
        String queryString = generateQueryString(params);
        // HMAC SHA256 서명 생성 (formData 전체를 대상으로)
        String signature = generateSignature(queryString, secretKey);

        Map<String, Object> res = webClient.delete()
                .uri(uriBuilder -> uriBuilder.path(endpoint)
                        .query(queryString + "&signature=" + signature)
                        .build())
                .headers(httpHeaders -> {
                    httpHeaders.add("X-MBX-APIKEY", accesskey);
                })
                .retrieve()
                .onStatus(status -> status.value() == 400, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    logger.error("Binance API 400 에러 발생: {}", errorBody);
                                    return Mono.error(new BinanceApiException("Binance API Bad Request (400): " + errorBody));
                                })
                )
                .bodyToMono(Map.class)
                .doOnSuccess(response -> logger.info("✅ Cancel Order 성공: {}", response))
                .doOnError(error -> logger.error("❌ Cancel Order 실패: {}", error.getMessage())).block();

        return res.get("status").toString();
    }

    /**
     * ✅ 주문 상태 조회 메서드
     * <p>
     * Binance 선물 주문 조회 API (GET /fapi/v1/order)를 호출하여 특정 주문의 상태를 확인합니다.
     * 필수 파라미터: symbol, orderId 필요합니다.
     * FILLED , CANCELED , NEW
     *
     * @param symbol 주문 조회 대상 심볼 (예: "BTCUSDT")
     * @return 주문 상태 응답 결과 (Mono<Map<String, Object>>)
     */
    public String orderStatus(String symbol, BigInteger orderId, String accesskey, String secretKey) {
        String endpoint = "/fapi/v1/order";
        Map<String, Object> params = new HashMap<>();
        long timestamp = getBinanceServerTime();
        params.put("timestamp", timestamp);
        params.put("symbol", symbol);
        params.put("orderId", orderId);

        // 주문 파라미터를 key=value 형식의 쿼리 스트링으로 생성
        String queryString = generateQueryString(params);
        // HMAC SHA256 서명 생성 (formData 전체를 대상으로)
        String signature = generateSignature(queryString, secretKey);

        Map<String, Object> res = webClient.get()
                .uri(uriBuilder -> uriBuilder.path(endpoint)
                        .query(queryString + "&signature=" + signature)
                        .build())
                .headers(httpHeaders -> {
                    httpHeaders.add("X-MBX-APIKEY", accesskey);
                })
                .retrieve()
                .onStatus(status -> status.value() == 400, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    logger.error("Binance API 400 에러 발생: {}", errorBody);
                                    return Mono.error(new BinanceApiException("Binance API Bad Request (400): " + errorBody));
                                })
                )
                .bodyToMono(Map.class)
                .doOnSuccess(response -> logger.info("✅ Status Order 성공: {}", response))
                .doOnError(error -> logger.error("❌ Status Order 실패: {}", error.getMessage())).block();

        return res.get("status").toString();
    }

    /**
     * HMAC SHA256 서명 생성 메서드
     *
     * @param queryString 서명할 문자열 (params가 연결된 form-data 쿼리 스트링)
     * @return 생성된 서명 (hex 문자열)
     */
    private String generateSignature(String queryString, String secretKey) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");

            sha256_HMAC.init(secretKeySpec);
            byte[] hash = sha256_HMAC.doFinal(queryString.getBytes(StandardCharsets.UTF_8));

            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            String result = formatter.toString();
            formatter.close();

            return result;

        } catch (Exception e) {
            throw new RuntimeException("서명 생성 오류", e);
        }
    }

    /**
     * 주문 파라미터 Map을 URL 인코딩되지 않은 쿼리 문자열(key=value&key=value)로 변환
     *
     * @param params 주문 파라미터 Map
     * @return 쿼리 문자열
     */
    private String generateQueryString(Map<String, Object> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }


    /**
     * 바이낸스 서버의 현재 시간을 가져오는 메소드
     * <p>
     * 바이낸스 선물 API의 "/fapi/v1/time" 엔드포인트를 호출하여
     * 서버의 현재 시간을 밀리초 단위로 가져옵니다.
     *
     * @return 서버 시간 (밀리초 단위)
     * @throws RuntimeException 서버 시간을 가져오지 못한 경우 예외 발생
     */
    private long getBinanceServerTime() {
        String endpoint = "/fapi/v1/time";
        Map<String, Object> result = webClient.get()
                .uri(endpoint)
                .retrieve()
                .onStatus(status -> status.value() == 400, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    logger.error("Binance API 400 에러 발생: {}", errorBody);
                                    return Mono.error(new BinanceApiException("Binance API Bad Request (400): " + errorBody));
                                })
                )
                .bodyToMono(Map.class)
                .block();
        if (result != null && result.containsKey("serverTime")) {
            return (long) result.get("serverTime");
        } else {
            throw new RuntimeException("Binance 서버 시간을 가져올 수 없습니다.");
        }
    }

    /**
     * exchangeInfo 응답에서 각 심볼의 정보를 파싱하여, 심볼명(key)별로
     * pricePrecision, minPrice, tickSize 값만을 가지는 Map을 반환합니다.
     *
     * @return 심볼별 정보 Map (key: 심볼명, value: {pricePrecision, minPrice, tickSize})
     */
    public Map<String, Integer> parseExchangeInfoForSymbols() {
        Map<String, Object> exchangeInfo = getExchangeInfo(); // 기존에 작성한 getExchangeInfo() 호출
        if (exchangeInfo == null || !exchangeInfo.containsKey("symbols")) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> symbols = (List<Map<String, Object>>) exchangeInfo.get("symbols");
        Map<String, Integer> result = new HashMap<>();

        for (Map<String, Object> symbolInfo : symbols) {
            String symbol = (String) symbolInfo.get("symbol");
            Integer stepSize = 0;
            List<Map<String, Object>> filters = (List<Map<String, Object>>) symbolInfo.get("filters");
            if (filters != null) {
                for (Map<String, Object> filter : filters) {
                    String filterType = (String) filter.get("filterType");
                    if ("LOT_SIZE".equals(filterType)) {
                        String stepSizeString = filter.get("stepSize").toString();
                        stepSize = countDecimalPlacesUsingBigDecimal(stepSizeString);
                        break;
                    }
                }
            }
            result.put(symbol, stepSize);
        }

        return result;
    }

    /**
     * 주어진 숫자 문자열을 BigDecimal로 변환한 후,
     * 불필요한 뒤의 0(trailing zeros)을 제거하여 소수점 이하 자릿수를 반환합니다.
     * 만약 소수점 이하 자릿수가 음수라면 0을 반환합니다.
     *
     * @param numberString 숫자를 나타내는 문자열 (예: "0.1000")
     * @return 소수점 이하 자리수 (예: "0.1000" → 1)
     */
    private Integer countDecimalPlacesUsingBigDecimal(String numberString) {
        BigDecimal bd = new BigDecimal(numberString);
        // BigDecimal에서 불필요한 후행 0을 제거한 후의 scale 값을 반환합니다.
        int scale = bd.stripTrailingZeros().scale();
        return scale < 0 ? 0 : scale;
    }

    /**
     * 바이낸스 선물 API의 "/fapi/v1/exchangeInfo" 엔드포인트를 호출하여
     * 거래소 정보를 가져옵니다.
     *
     * @return 거래소 정보 (Map 형태)
     * @throws RuntimeException 거래소 정보를 가져오지 못한 경우 예외 발생
     */
    private Map<String, Object> getExchangeInfo() {
        String endpoint = "/fapi/v1/exchangeInfo";

        Map<String, Object> result = webClient.get()
                .uri(endpoint)
                .retrieve()
                .onStatus(status -> status.value() == 400, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    logger.error("Binance API 400 에러 발생: {}", errorBody);
                                    return Mono.error(new BinanceApiException("Binance API Bad Request (400): " + errorBody));
                                })
                )
                .bodyToMono(Map.class)
                .block();

        if (result == null) {
            throw new RuntimeException("Binance 거래소 정보를 가져올 수 없습니다.");
        }
        return result;
    }

    /**
     * 바이낸스 선물 API의 "/fapi/v1/premiumIndex" 엔드포인트를 호출하여
     * markPrice (시장가) 정보를 가져옵니다.
     *
     * @param symbol 심볼 (예: BTCUSDT). null 또는 빈 문자열일 경우 전체 데이터를 반환할 수 있음.
     * @return markPrice (시장가)
     * @throws RuntimeException 마켓 가격 정보를 가져오지 못한 경우 예외 발생
     */
    public BigDecimal getMarkPrice(String symbol) {
        String endpoint = "/fapi/v1/premiumIndex";

        // symbol이 제공되면 쿼리 파라미터로 추가
        Map<String, Object> params = new HashMap<>();
        params.put("symbol", symbol);

        // 주문 파라미터를 key=value 형식의 쿼리 스트링으로 생성
        String queryString = generateQueryString(params);

        // 웹 클라이언트로 API 호출
        Object responseData = webClient.get()
                .uri(uriBuilder -> uriBuilder.path(endpoint)
                        .query(queryString)
                        .build())
                .retrieve()
                .onStatus(status -> status.value() == 400, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    logger.error("Binance API 400 에러 발생: {}", errorBody);
                                    return Mono.error(new BinanceApiException("Binance API Bad Request (400): " + errorBody));
                                })
                )
                // symbol이 제공되면 Map, 없으면 List로 반환할 수 있음
                .bodyToMono(Object.class)
                .block();

        if (responseData == null) {
            throw new RuntimeException("Binance 마켓 가격 정보를 가져올 수 없습니다.");
        }

        // symbol 파라미터가 있는 경우 단일 Map으로 처리
        if (symbol != null && !symbol.isEmpty()) {
            Map<String, Object> result = (Map<String, Object>) responseData;
            return new BigDecimal(result.get("markPrice").toString());
        } else {
            // symbol이 없으면 배열 형태로 반환되므로 첫 번째 항목에서 markPrice를 추출
            List<Map<String, Object>> results = (List<Map<String, Object>>) responseData;
            if (results.isEmpty()) {
                throw new RuntimeException("Mark Price 정보를 찾을 수 없습니다.");
            }
            return new BigDecimal(results.get(0).get("markPrice").toString());
        }
    }
}
