package com.project.sneakermarket.auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.sneakermarket.auction.entity.AuctionOrder;

public interface AuctionOrderRepository extends JpaRepository<AuctionOrder, Long> {
    Optional<AuctionOrder> findByAuctionId(Long auctionId);
    boolean existsByAuctionId(Long auctionId);
}
