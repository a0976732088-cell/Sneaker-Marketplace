package com.project.sneakermarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SneakerMarket 後端應用程式進入點。
 *
 * EnableScheduling 先在 BASE 開啟，之後競標系統可用於自動結標排程。
 */
@SpringBootApplication
@EnableScheduling
public class SneakerMarketBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SneakerMarketBackendApplication.class, args);
    }
}
