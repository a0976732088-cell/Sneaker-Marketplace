package com.project.sneakermarket.auction.email;

import java.time.LocalDateTime;
import java.util.List;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.auction.service.AuctionTime;
import com.project.sneakermarket.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionEmailService {
    private final AuctionResultRepository results;
    private final AuctionOrderRepository orders;
    private final AuctionRepository auctions;
    private final AuctionProductRepository products;
    private final AuctionBidRepository bids;
    private final MemberRepository members;
    private final AuctionEmailLogRepository logs;
    private final AuctionTime time;
    private final AuctionEmailSettings settings;

    /** 一場競標所有收件工作原子建立；Result lock 序列化 Event 與 Recovery。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Long> ensureEmailLogsForAuction(Long auctionId) {
        if (!settings.enabled()) return List.of();
        AuctionResult result = results.findByAuctionIdForUpdate(auctionId).orElse(null);
        if (result == null) return List.of();
        boolean sold = result.getResultStatus() == AuctionResultStatus.SOLD;
        if (!sold && result.getResultStatus() != AuctionResultStatus.UNSOLD) return List.of();
        // SOLD 缺 Order 是合法的等待狀態，連賣家工作也不能提早建立。
        if (sold && !orders.existsByAuctionId(auctionId)) return List.of();
        Auction auction = auctions.findById(auctionId).orElseThrow();
        AuctionProduct product = products.findById(auction.getAuctionProductId()).orElseThrow();
        ensureLog(auctionId, product.getSellerId(), AuctionEmailRecipientRole.SELLER,
                sold ? AuctionEmailType.SOLD_SELLER : AuctionEmailType.UNSOLD_SELLER, product.getProductName());
        if (sold) {
            ensureLog(auctionId, result.getWinnerId(), AuctionEmailRecipientRole.WINNER,
                    AuctionEmailType.WON, product.getProductName());
            for (Long bidderId : bids.findDistinctLosingBidderIds(auctionId, result.getWinnerId())) {
                ensureLog(auctionId, bidderId, AuctionEmailRecipientRole.BIDDER,
                        AuctionEmailType.LOST, product.getProductName());
            }
        }
        logs.flush();
        return logs.findDispatchCandidateIds(auctionId, settings.maxRetries());
    }

    /** 與訂單棄標狀態同交易建立 outbox 工作；SMTP 只在提交後執行。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void ensureAbandonedEmailLogs(AuctionOrder order) {
        if (!settings.enabled()) return;
        if (order == null || order.getOrderStatus() != AuctionOrderStatus.ABANDONED
                || order.getAbandonedAt() == null) return;

        Auction auction = auctions.findById(order.getAuctionId()).orElseThrow();
        AuctionProduct product = products.findById(auction.getAuctionProductId()).orElseThrow();
        ensureLog(order.getAuctionId(), order.getBuyerId(), AuctionEmailRecipientRole.WINNER,
                AuctionEmailType.ORDER_ABANDONED_BUYER, product.getProductName(), order.getAbandonedAt());
        ensureLog(order.getAuctionId(), order.getSellerId(), AuctionEmailRecipientRole.SELLER,
                AuctionEmailType.ORDER_ABANDONED_SELLER, product.getProductName(), order.getAbandonedAt());
        logs.flush();
    }

    private void ensureLog(Long auctionId, Long memberId, AuctionEmailRecipientRole role,
            AuctionEmailType type, String productName) {
        ensureLog(auctionId, memberId, role, type, productName, time.now());
    }

    private void ensureLog(Long auctionId, Long memberId, AuctionEmailRecipientRole role,
            AuctionEmailType type, String productName, LocalDateTime createdAt) {
        if (logs.existsByAuctionIdAndRecipientMemberIdAndEmailType(auctionId, memberId, type)) return;
        var member = members.findById(memberId).orElseThrow();
        var log = new AuctionEmailLog();
        log.setAuctionId(auctionId);
        log.setRecipientMemberId(memberId);
        log.setRecipientEmail(member.getEmail());
        log.setRecipientRole(role);
        log.setEmailType(type);
        String subject = "酷客 Kicks " + outcome(type) + "：" + productName;
        subject = subject.replaceAll("[\\r\\n]+", " ");
        log.setSubject(subject.substring(0, Math.min(200, subject.length())));
        log.setSendStatus(AuctionEmailSendStatus.PENDING);
        log.setRetryCount(0);
        log.setCreatedAt(createdAt);
        log.setUpdatedAt(createdAt);
        logs.save(log);
    }

    static String outcome(AuctionEmailType type) {
        return switch (type) {
            case SOLD_SELLER -> "商品已成交";
            case UNSOLD_SELLER -> "商品流標";
            case WON -> "恭喜得標";
            case LOST -> "本次未得標";
            case ORDER_ABANDONED_BUYER -> "競標訂單已因逾期未付款而棄標";
            case ORDER_ABANDONED_SELLER -> "競標訂單買家逾期未付款";
        };
    }
}
