package com.example.autotradebot.controller;

import com.example.autotradebot.service.mexc.MexcService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/mexc")
public class MexcController {

    private final MexcService mexcService;

    public MexcController(MexcService mexcService) {
        this.mexcService = mexcService;
    }

    // 📌 1. 레버리지 설정
    @PostMapping("/set-leverage")
    public Mono<ResponseEntity<String>> setLeverage(@RequestParam String symbol, @RequestParam int leverage) {
        return mexcService.setLeverage(symbol, leverage)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> handleError("Error setting leverage", e));
    }

    // 📌 2. 롱(매수) 주문 실행
    @PostMapping("/long")
    public Mono<ResponseEntity<String>> placeLongOrder(@RequestParam String symbol, @RequestParam String quantity) {
        return mexcService.placeLongOrder(symbol, quantity)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> handleError("Error placing long order", e));
    }

    // 📌 3. 숏(매도) 주문 실행
    @PostMapping("/short")
    public Mono<ResponseEntity<String>> placeShortOrder(@RequestParam String symbol, @RequestParam String quantity) {
        return mexcService.placeShortOrder(symbol, quantity)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> handleError("Error placing short order", e));
    }

    // 📌 4. 포지션 조회
    @GetMapping("/position")
    public Mono<ResponseEntity<String>> getPosition(@RequestParam String symbol) {
        return mexcService.getPosition(symbol)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> handleError("Error fetching position", e));
    }

    // 📌 5. 롱 포지션 종료
    @PostMapping("/close-long")
    public Mono<ResponseEntity<String>> closeLongPosition(@RequestParam String symbol, @RequestParam String quantity) {
        return mexcService.closeLongPosition(symbol, quantity)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> handleError("Error closing long position", e));
    }

    // 📌 6. 숏 포지션 종료
    @PostMapping("/close-short")
    public Mono<ResponseEntity<String>> closeShortPosition(@RequestParam String symbol, @RequestParam String quantity) {
        return mexcService.closeShortPosition(symbol, quantity)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> handleError("Error closing short position", e));
    }

    // 📌 7. 레퍼럴 계정 조회
    @GetMapping("/referral")
    public Mono<ResponseEntity<String>> getReferralAccount() {
        return mexcService.getReferralAccount()
                .map(ResponseEntity::ok)
                .onErrorResume(e -> handleError("Error fetching referral account", e));
    }

    /**
     * 📌 공통 에러 핸들링 (비동기 방식)
     */
    private Mono<ResponseEntity<String>> handleError(String message, Throwable e) {
        return Mono.just(ResponseEntity.internalServerError().body("❌ " + message + ": " + e.getMessage()));
    }
}
