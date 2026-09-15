package com.project.sneakermarket.auction.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.sneakermarket.auction.entity.AuctionOrder;

public interface AuctionOrderRepository extends JpaRepository<AuctionOrder, Long> {
    Optional<AuctionOrder> findByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select orders from AuctionOrder orders where orders.orderNumber = :orderNumber")
    Optional<AuctionOrder> findByOrderNumberForUpdate(@Param("orderNumber") String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select orders from AuctionOrder orders where orders.auctionOrderId = :orderId")
    Optional<AuctionOrder> findByIdForUpdate(@Param("orderId") Long orderId);

    @Query("""
            select orders.auctionOrderId from AuctionOrder orders
            where orders.paymentStatus = com.project.sneakermarket.auction.entity.AuctionPaymentStatus.PENDING
              and orders.orderStatus = com.project.sneakermarket.auction.entity.AuctionOrderStatus.PENDING_PAYMENT
              and orders.paymentDueAt is not null
              and orders.paymentDueAt <= :now
            order by orders.auctionOrderId
            """)
    List<Long> findPaymentDeadlineCandidateIds(@Param("now") LocalDateTime now);

    Page<AuctionOrder> findByBuyerId(Long buyerId, Pageable pageable);
    Page<AuctionOrder> findBySellerId(Long sellerId, Pageable pageable);
    Optional<AuctionOrder> findByAuctionId(Long auctionId);
    List<AuctionOrder> findByAuctionIdIn(List<Long> auctionIds);
    boolean existsByAuctionId(Long auctionId);
}
