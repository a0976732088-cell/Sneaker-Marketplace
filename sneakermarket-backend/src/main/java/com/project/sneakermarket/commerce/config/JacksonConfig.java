package com.project.sneakermarket.commerce.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // Spring Boot 4 只會自動配置 Jackson 3 的 JsonMapper，
    // 不再自動建立 Jackson 2 的 ObjectMapper Bean，故需手動宣告
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}