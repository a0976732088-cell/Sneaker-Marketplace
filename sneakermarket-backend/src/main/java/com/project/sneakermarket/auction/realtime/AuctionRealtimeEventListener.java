package com.project.sneakermarket.auction.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 主交易成功提交後，將既有公開 snapshot 發送到單場競標 Topic。 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionRealtimeEventListener {

    private static final String AUCTION_TOPIC_PREFIX = "/topic/auctions/";

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionRealtimeEvent(AuctionRealtimeEvent event) {
        AuctionRealtimeMessage message = event.message();
        Long auctionId = message.data().auctionId();
        try {
            messagingTemplate.convertAndSend(AUCTION_TOPIC_PREFIX + auctionId, message);
        } catch (RuntimeException ex) {
            // 主交易已提交；即時廣播失敗不可把成功的 REST 商業操作改報為失敗。
            log.error("Auction realtime broadcast failed: auctionId={}, eventType={}",
                    auctionId, message.eventType(), ex);
        }
    }
}
