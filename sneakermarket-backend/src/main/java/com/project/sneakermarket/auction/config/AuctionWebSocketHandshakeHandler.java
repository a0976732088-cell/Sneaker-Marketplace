package com.project.sneakermarket.auction.config;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import jakarta.servlet.http.HttpSession;

/** 將正式 HttpSession 會員識別安全地帶入 WebSocket session。 */
@Component
public class AuctionWebSocketHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    public Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return null;
        }

        HttpSession session = servletRequest.getServletRequest().getSession(false);
        if (session == null) {
            return null;
        }

        Object sessionMemberId;
        try {
            sessionMemberId = session.getAttribute("memberId");
        } catch (IllegalStateException ex) {
            // Session 可能在握手與登出競態中被 invalidate；仍以匿名身分放行 public socket。
            return null;
        }
        if (!(sessionMemberId instanceof Long memberId) || memberId <= 0) {
            return null;
        }

        return () -> memberId.toString();
    }
}
