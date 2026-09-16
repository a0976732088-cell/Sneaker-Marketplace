package com.project.sneakermarket.auction.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/** 僅保護競標會員私人通知，不改變匿名 public topic 訂閱。 */
@Component
public class AuctionPrivateSubscriptionInterceptor implements ChannelInterceptor {

    public static final String PRIVATE_DESTINATION = "/user/queue/auction-events";
    public static final String PRIVATE_BROKER_DESTINATION = "/queue/auction-events";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );
        if (accessor == null) {
            return message;
        }

        String destination = accessor.getDestination();
        if (!isAuctionPrivateDestination(destination)) {
            return message;
        }

        if (accessor.getCommand() == StompCommand.SEND) {
            throw new MessageDeliveryException(message, "Client publishing to auction private events is forbidden");
        }
        if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        // 禁止把 memberId 塞進 destination；client 只能使用標準 user destination。
        if (!PRIVATE_DESTINATION.equals(destination)) {
            throw new MessageDeliveryException(message, "Invalid auction private subscription destination");
        }

        if (!hasValidMemberPrincipal(accessor)) {
            throw new MessageDeliveryException(message, "Authentication is required for auction private events");
        }

        return message;
    }

    private boolean isAuctionPrivateDestination(String destination) {
        return destination != null
                && (destination.startsWith(PRIVATE_BROKER_DESTINATION)
                || (destination.startsWith("/user/")
                && destination.endsWith(PRIVATE_BROKER_DESTINATION)));
    }

    private boolean hasValidMemberPrincipal(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null || accessor.getUser().getName() == null) {
            return false;
        }
        try {
            return Long.parseLong(accessor.getUser().getName()) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
