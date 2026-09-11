package com.project.sneakermarket.auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.project.sneakermarket.auction.entity.AuctionOrder;

public interface AuctionOrderRepository extends JpaRepository<AuctionOrder, Long> {
    Page<AuctionOrder> findByBuyerId(Long buyerId, Pageable pageable);
    Optional<AuctionOrder> findByAuctionId(Long auctionId);
    boolean existsByAuctionId(Long auctionId);
}
