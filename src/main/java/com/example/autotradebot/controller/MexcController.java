//package com.example.autotradebot.controller;
//
//import com.example.autotradebot.service.mexc.MexcService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.IOException;
//
//@RestController
//@RequestMapping("/mexc")
//public class MexcController {
//
//    private final MexcService mexcService;
//
//    public MexcController(MexcService mexcService) {
//        this.mexcService = mexcService;
//    }
//
//    // 📌 1. 레버리지 설정
//    @PostMapping("/set-leverage")
//    public ResponseEntity<String> setLeverage(@RequestParam String symbol, @RequestParam int leverage) {
//        try {
//            String response = mexcService.setLeverage(symbol, leverage);
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().body("Error setting leverage: " + e.getMessage());
//        }
//    }
//
//    // 📌 2. 롱(매수) 주문 실행
//    @PostMapping("/long")
//    public ResponseEntity<String> placeLongOrder(@RequestParam String symbol, @RequestParam String quantity) {
//        try {
//            String response = mexcService.placeLongOrder(symbol, quantity);
//            return ResponseEntity.ok(response);
//        } catch (IOException e) {
//            return ResponseEntity.internalServerError().body("Error placing long order: " + e.getMessage());
//        }
//    }
//
//    // 📌 3. 숏(매도) 주문 실행
//    @PostMapping("/short")
//    public ResponseEntity<String> placeShortOrder(@RequestParam String symbol, @RequestParam String quantity) {
//        try {
//            String response = mexcService.placeShortOrder(symbol, quantity);
//            return ResponseEntity.ok(response);
//        } catch (IOException e) {
//            return ResponseEntity.internalServerError().body("Error placing short order: " + e.getMessage());
//        }
//    }
//
//    // 📌 4. 포지션 조회
//    @GetMapping("/position")
//    public ResponseEntity<String> getPosition(@RequestParam String symbol) {
//        try {
//            String response = mexcService.getPosition(symbol);
//            return ResponseEntity.ok(response);
//        } catch (IOException e) {
//            return ResponseEntity.internalServerError().body("Error fetching position: " + e.getMessage());
//        }
//    }
//
//    // 📌 5. 롱 포지션 종료
//    @PostMapping("/close-long")
//    public ResponseEntity<String> closeLongPosition(@RequestParam String symbol, @RequestParam String quantity) {
//        try {
//            String response = mexcService.closeLongPosition(symbol, quantity);
//            return ResponseEntity.ok(response);
//        } catch (IOException e) {
//            return ResponseEntity.internalServerError().body("Error closing long position: " + e.getMessage());
//        }
//    }
//
//    // 📌 6. 숏 포지션 종료
//    @PostMapping("/close-short")
//    public ResponseEntity<String> closeShortPosition(@RequestParam String symbol, @RequestParam String quantity) {
//        try {
//            String response = mexcService.closeShortPosition(symbol, quantity);
//            return ResponseEntity.ok(response);
//        } catch (IOException e) {
//            return ResponseEntity.internalServerError().body("Error closing short position: " + e.getMessage());
//        }
//    }
//
//    // 📌 7. 레퍼럴 계정 조회
//    @GetMapping("/referral")
//    public ResponseEntity<String> getReferralAccount() {
//        try {
//            String response = mexcService.getReferralAccount();
//            return ResponseEntity.ok(response);
//        } catch (IOException e) {
//            return ResponseEntity.internalServerError().body("Error fetching referral account: " + e.getMessage());
//        }
//    }
//}
