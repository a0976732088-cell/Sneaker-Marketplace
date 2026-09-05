package com.project.sneakermarket.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * BASE 開發階段先開放所有 API，讓各模組可以平行開發。
     *
     * 專案目前採用 HttpSession 保存登入會員狀態，
     * 因此允許 Spring 在需要時建立與使用 Session。
     *
     * 後續會員模組完成登入與角色權限後，
     * 再補上 MEMBER / ADMIN 等正式授權規則。
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                // 目前前後端開發階段先停用 CSRF，
                // 正式部署若使用 Session Cookie，需再評估 CSRF 防護策略。
                .csrf(csrf -> csrf.disable())

                // 使用既有 CorsConfig
                .cors(Customizer.withDefaults())

                // 需要時才建立 Session，允許登入狀態保存在 HttpSession。
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                // BASE 階段仍先開放所有請求。
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().permitAll()
                )

                .build();
    }
}