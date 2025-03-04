package com.example.autotradebot.service.mexc;

import com.example.autotradebot.util.MexcHttpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class MexcService {

    private final MexcHttpUtil httpUtil;

    @Value("${mexc.api.key}")
    private String API_KEY;

    @Value("${mexc.api.secret}")
    private String SECRET_KEY;

    private static final String BASE_URL = "https://futures.mexc.com";

    public MexcService(MexcHttpUtil httpUtil) {
        this.httpUtil = httpUtil;
    }

    /**
     * 📌 1. 레버리지 설정 (서명 필요)
     */
    public Mono<String> setLeverage(String symbol, int leverage) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("symbol", symbol);
        payload.put("leverage", leverage);

        return httpUtil.sendSignedPostRequest(BASE_URL + "/api/v1/position/leverage", payload, API_KEY, SECRET_KEY);
    }

    /**
     * 📌 2. 롱(매수) 주문 실행
     */
    public Mono<String> placeLongOrder(String symbol, String quantity) {
        return placeOrder(symbol, "BUY", quantity);
    }

    /**
     * 📌 3. 숏(매도) 주문 실행
     */
    public Mono<String> placeShortOrder(String symbol, String quantity) {
        return placeOrder(symbol, "SELL", quantity);
    }

    /**
     * 📌 4. 포지션 정보 조회 (서명 필요)
     */
    public Mono<String> getPosition(String symbol) {
        Map<String, String> params = new HashMap<>();
        params.put("symbol", symbol);
        return httpUtil.sendSignedGetRequest(BASE_URL + "/api/v1/position", params, API_KEY, SECRET_KEY);
    }

    /**
     * 📌 5. 롱 포지션 종료 (숏 주문)
     */
    public Mono<String> closeLongPosition(String symbol, String quantity) {
        return placeShortOrder(symbol, quantity);
    }

    /**
     * 📌 6. 숏 포지션 종료 (롱 주문)
     */
    public Mono<String> closeShortPosition(String symbol, String quantity) {
        return placeLongOrder(symbol, quantity);
    }

    /**
     * 📌 7. 레퍼럴 계정 조회 (서명 필요)
     */
    public Mono<String> getReferralAccount() {
        return httpUtil.sendSignedGetRequest(BASE_URL + "/api/v1/referer/account", new HashMap<>(), API_KEY, SECRET_KEY);
    }

    /**
     * 📌 8. 주문 실행 (롱/숏 공통, 서명 필요)
     */
    private Mono<String> placeOrder(String symbol, String side, String quantity) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("symbol", symbol);
        payload.put("side", side);
        payload.put("type", "MARKET");
        payload.put("quantity", quantity);

        return httpUtil.sendSignedPostRequest(BASE_URL + "/api/v1/order", payload, API_KEY, SECRET_KEY);
    }
}
