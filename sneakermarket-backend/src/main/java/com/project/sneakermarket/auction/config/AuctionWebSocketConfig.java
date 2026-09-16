package com.project.sneakermarket.auction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/** 競標公開 topic 與會員私人通知共用的 STOMP/WebSocket 邊界設定。 */
@Configuration
@EnableWebSocketMessageBroker
public class AuctionWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String[] allowedOrigins;
    private final AuctionWebSocketHandshakeHandler handshakeHandler;
    private final AuctionPrivateSubscriptionInterceptor privateSubscriptionInterceptor;

    public AuctionWebSocketConfig(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String[] allowedOrigins,
            AuctionWebSocketHandshakeHandler handshakeHandler,
            AuctionPrivateSubscriptionInterceptor privateSubscriptionInterceptor) {
        this.allowedOrigins = allowedOrigins;
        this.handshakeHandler = handshakeHandler;
        this.privateSubscriptionInterceptor = privateSubscriptionInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setHandshakeHandler(handshakeHandler)
                .setAllowedOrigins(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        registry.setPreservePublishOrder(true);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(privateSubscriptionInterceptor);
    }
}
