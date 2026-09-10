package com.project.sneakermarket.auction.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

/** 消費已提交的結標結果；絕不改寫 Auction 或 AuctionResult。 */
@Service
@RequiredArgsConstructor
public class AuctionOrderService {
    private final AuctionResultRepository results;
    private final AuctionOrderRepository orders;
    private final AuctionRepository auctions;
    private final AuctionProductRepository products;
    private final MemberRepository members;
    private final AuctionTime time;

    // 訂單不加入呼叫端交易；呼叫端只應傳入已 commit 的 Result 所屬 auctionId。
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createOrderForAuction(Long auctionId) {
        AuctionResult result = results.findByAuctionIdForUpdate(auctionId).orElse(null);
        if (result == null || orders.existsByAuctionId(auctionId)) return;
        if (result.getResultStatus() != AuctionResultStatus.SOLD) return;
        if (result.getWinnerId() == null || result.getFinalPrice() == null
                || result.getFinalPrice().signum() <= 0) {
            throw invalid(auctionId, "missing winner or invalid final price");
        }

        Auction auction = auctions.findById(auctionId)
                .orElseThrow(() -> invalid(auctionId, "auction missing"));
        if (auction.getStatus() != AuctionStatus.SOLD
                || !result.getWinnerId().equals(auction.getHighestBidderId())
                || auction.getCurrentPrice() == null
                || result.getFinalPrice().compareTo(auction.getCurrentPrice()) != 0) {
            throw invalid(auctionId, "auction and result disagree");
        }
        AuctionProduct product = products.findById(auction.getAuctionProductId())
                .orElseThrow(() -> invalid(auctionId, "product missing"));
        if (product.getSellerId() == null || !members.existsById(product.getSellerId())) {
            throw invalid(auctionId, "seller missing");
        }
        Member winner = members.findById(result.getWinnerId())
                .orElseThrow(() -> invalid(auctionId, "winner missing"));
        requireText(winner.getName(), 50, auctionId, "receiver name");
        requireText(winner.getPhone(), 20, auctionId, "receiver phone");
        requireText(winner.getAddress(), 300, auctionId, "shipping address");
        requireText(product.getPaymentMethod(), 50, auctionId, "payment method");

        LocalDateTime now = time.now();
        AuctionOrder order = new AuctionOrder();
        // 35 字元，無須依賴尚未產生的 DB ID；UNIQUE(order_number) 為碰撞防線。
        order.setOrderNumber("AO-" + UUID.randomUUID().toString().replace("-", ""));
        order.setAuctionId(auctionId);
        order.setBuyerId(result.getWinnerId());
        order.setSellerId(product.getSellerId());
        order.setFinalPrice(result.getFinalPrice());
        order.setPaymentMethod(product.getPaymentMethod());
        order.setPaymentStatus(AuctionPaymentStatus.PENDING);
        order.setShippingStatus(AuctionShippingStatus.NOT_SHIPPED);
        order.setOrderStatus(AuctionOrderStatus.PENDING_PAYMENT);
        order.setReceiverName(winner.getName());
        order.setReceiverPhone(winner.getPhone());
        order.setShippingAddress(winner.getAddress());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orders.saveAndFlush(order);
    }

    private void requireText(String value, int maxLength, Long auctionId, String field) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw invalid(auctionId, "missing or invalid " + field);
        }
    }

    private IllegalStateException invalid(Long auctionId, String reason) {
        // 不將會員收件內容寫入錯誤日誌。
        return new IllegalStateException("Cannot create auction order: auctionId=" + auctionId + ", " + reason);
    }
}
