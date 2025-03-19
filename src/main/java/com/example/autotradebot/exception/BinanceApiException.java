package com.example.autotradebot.exception;

public class BinanceApiException extends RuntimeException {

    public BinanceApiException() {
        super();
    }

    public BinanceApiException(String message) {
        super(message);
    }

    public BinanceApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public BinanceApiException(Throwable cause) {
        super(cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // 스택 트레이스를 채우지 않고, 현재 객체를 반환합니다.
        return this;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        // 빈 배열을 반환하여 스택 트레이스를 비활성화합니다.
        return new StackTraceElement[0];
    }
}
