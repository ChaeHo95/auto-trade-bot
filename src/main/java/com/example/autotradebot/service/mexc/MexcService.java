//package com.example.autotradebot.service.mexc;
//
//import com.example.autotradebot.util.MexcHttpUtil;
//import com.example.autotradebot.util.MexcSignatureUtil;
//import com.google.gson.JsonObject;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//public class MexcService {
//
//    private final MexcHttpUtil httpUtil;
//
//    @Value("${mexc.api.key}")
//    private String API_KEY;
//
//    @Value("${mexc.api.secret}")
//    private String SECRET_KEY;
//
//    private static final String BASE_URL = "https://futures.mexc.com";
//
//    public MexcService(MexcHttpUtil httpUtil) {
//        this.httpUtil = httpUtil;
//    }
//
//    /**
//     * 📌 1. 레버리지 설정 (서명 필요)
//     */
//    public String setLeverage(String symbol, int leverage) throws Exception {
//        long timestamp = System.currentTimeMillis();
//
//        JsonObject requestJson = new JsonObject();
//        requestJson.addProperty("symbol", symbol);
//        requestJson.addProperty("leverage", leverage);
//        requestJson.addProperty("timestamp", timestamp);
//
//        String signature = MexcSignatureUtil.generateSignature("symbol=" + symbol + "&leverage=" + leverage + "&timestamp=" + timestamp, SECRET_KEY);
//
//        return httpUtil.sendSignedPostRequest(BASE_URL + "/api/v1/position/leverage", requestJson.toString(), API_KEY, SECRET_KEY);
//    }
//
//    /**
//     * 📌 2. 롱(매수) 주문 실행
//     */
//    public String placeLongOrder(String symbol, String quantity) throws IOException {
//        return placeOrder(symbol, "BUY", quantity);
//    }
//
//    /**
//     * 📌 3. 숏(매도) 주문 실행
//     */
//    public String placeShortOrder(String symbol, String quantity) throws IOException {
//        return placeOrder(symbol, "SELL", quantity);
//    }
//
//    /**
//     * 📌 4. 포지션 정보 조회 (서명 필요)
//     */
//    public String getPosition(String symbol) throws IOException {
//        Map<String, String> params = new HashMap<>();
//        params.put("symbol", symbol);
//        return httpUtil.sendSignedGetRequest(BASE_URL + "/api/v1/position", params, API_KEY, SECRET_KEY);
//    }
//
//    /**
//     * 📌 5. 롱 포지션 종료 (숏 주문)
//     */
//    public String closeLongPosition(String symbol, String quantity) throws IOException {
//        return placeShortOrder(symbol, quantity);
//    }
//
//    /**
//     * 📌 6. 숏 포지션 종료 (롱 주문)
//     */
//    public String closeShortPosition(String symbol, String quantity) throws IOException {
//        return placeLongOrder(symbol, quantity);
//    }
//
//    /**
//     * 📌 7. 레퍼럴 계정 조회 (서명 필요)
//     */
//    public String getReferralAccount() throws IOException {
//        return httpUtil.sendSignedGetRequest(BASE_URL + "/api/v1/referer/account", new HashMap<>(), API_KEY, SECRET_KEY);
//    }
//
//    /**
//     * 📌 8. 주문 실행 (롱/숏 공통, 서명 필요)
//     */
//    private String placeOrder(String symbol, String side, String quantity) throws IOException {
//        try {
//            long timestamp = System.currentTimeMillis();
//
//            JsonObject order = new JsonObject();
//            order.addProperty("symbol", symbol);
//            order.addProperty("side", side);
//            order.addProperty("type", "MARKET");
//            order.addProperty("quantity", quantity);
//            order.addProperty("timestamp", timestamp);
//
//            // 서명 생성 (timestamp 포함)
//            String payload = "symbol=" + symbol + "&side=" + side + "&type=MARKET&quantity=" + quantity + "&timestamp=" + timestamp;
//            String signature = MexcSignatureUtil.generateSignature(payload, SECRET_KEY);
//
//            return httpUtil.sendSignedPostRequest(BASE_URL + "/api/v1/order", order.toString(), API_KEY, SECRET_KEY);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Signature Error: " + e.getMessage();
//        }
//    }
//}
