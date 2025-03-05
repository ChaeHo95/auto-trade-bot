package com.example.autotradebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutoTradeBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoTradeBotApplication.class, args);
    }

}

