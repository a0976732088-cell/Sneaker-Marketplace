package com.project.sneakermarket.auction.config;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.security.Principal;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

class AuctionPrivateSubscriptionInterceptorTests {

    private final AuctionPrivateSubscriptionInterceptor interceptor =
            new AuctionPrivateSubscriptionInterceptor();
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void anonymousPublicAuctionTopicSubscriptionRemainsAllowed() {
        Message<byte[]> message = subscription("/topic/auctions/123", null);

        assertSame(message, interceptor.preSend(message, channel));
    }

    @Test
    void anonymousStandardPrivateSubscriptionIsRejected() {
        Message<byte[]> message = subscription(
                AuctionPrivateSubscriptionInterceptor.PRIVATE_DESTINATION,
                null
        );

        assertThrows(
                MessageDeliveryException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    @Test
    void authenticatedStandardPrivateSubscriptionIsAllowed() {
        Message<byte[]> message = subscription(
                AuctionPrivateSubscriptionInterceptor.PRIVATE_DESTINATION,
                () -> "123"
        );

        assertSame(message, interceptor.preSend(message, channel));
    }

    @Test
    void invalidPrincipalNamesAreRejectedForPrivateSubscription() {
        for (String principalName : new String[]{" ", "member-123", "0", "-1"}) {
            Message<byte[]> message = subscription(
                    AuctionPrivateSubscriptionInterceptor.PRIVATE_DESTINATION,
                    () -> principalName
            );

            assertThrows(
                    MessageDeliveryException.class,
                    () -> interceptor.preSend(message, channel),
                    () -> "unexpectedly allowed principal=" + principalName
            );
        }
    }

    @Test
    void clientCannotSelectAnotherMemberInPrivateDestination() {
        Message<byte[]> message = subscription(
                "/user/999/queue/auction-events",
                () -> "123"
        );

        assertThrows(
                MessageDeliveryException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    @Test
    void directQueueDestinationCannotBypassUserDestinationProtection() {
        Message<byte[]> message = subscription("/queue/auction-events", () -> "123");

        assertThrows(
                MessageDeliveryException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    @Test
    void resolvedUserQueueDestinationCannotBypassUserDestinationProtection() {
        Message<byte[]> message = subscription(
                "/queue/auction-events-user-session-123",
                () -> "123"
        );

        assertThrows(
                MessageDeliveryException.class,
                () -> interceptor.preSend(message, channel)
        );
    }

    @Test
    void clientSendToAuctionPrivateDestinationIsAlwaysRejected() {
        Message<byte[]> selectedMember = message(
                StompCommand.SEND,
                "/user/999/queue/auction-events",
                () -> "123"
        );
        Message<byte[]> rawBroker = message(
                StompCommand.SEND,
                "/queue/auction-events-user-session-999",
                () -> "123"
        );

        assertThrows(
                MessageDeliveryException.class,
                () -> interceptor.preSend(selectedMember, channel)
        );
        assertThrows(
                MessageDeliveryException.class,
                () -> interceptor.preSend(rawBroker, channel)
        );
    }

    @Test
    void interceptorDoesNotBroadenItsScopeToOtherDestination() {
        Message<byte[]> otherQueue = subscription("/queue/other-events", null);

        assertSame(otherQueue, interceptor.preSend(otherQueue, channel));
    }

    private Message<byte[]> subscription(String destination, Principal principal) {
        return message(StompCommand.SUBSCRIBE, destination, principal);
    }

    private Message<byte[]> message(
            StompCommand command,
            String destination,
            Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
