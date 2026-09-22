package com.project.sneakermarket.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration defaultConfig = new CorsConfiguration();
        defaultConfig.setAllowedOrigins(Arrays.asList(allowedOrigins));
        defaultConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        defaultConfig.setAllowedHeaders(List.of("*"));
        defaultConfig.setAllowCredentials(true);
        source.registerCorsConfiguration("/api/**", defaultConfig);

        return source;
    }
}