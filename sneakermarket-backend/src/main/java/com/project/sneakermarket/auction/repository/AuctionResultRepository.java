package com.project.sneakermarket.auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.sneakermarket.auction.entity.AuctionResult;

public interface AuctionResultRepository extends JpaRepository<AuctionResult, Long> {

    Optional<AuctionResult> findByAuctionId(Long auctionId);
}
