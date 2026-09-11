package com.project.sneakermarket.auction.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.sneakermarket.auction.entity.AuctionResult;

import jakarta.persistence.LockModeType;

public interface AuctionResultRepository extends JpaRepository<AuctionResult, Long> {

    Optional<AuctionResult> findByAuctionId(Long auctionId);

    List<AuctionResult> findByAuctionIdIn(List<Long> auctionIds);

    /** 建單交易先鎖定正式 Result，再檢查訂單是否存在。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AuctionResult r where r.auctionId = :auctionId")
    Optional<AuctionResult> findByAuctionIdForUpdate(@Param("auctionId") Long auctionId);

    @Query("""
            select r.auctionId from AuctionResult r
            where r.resultStatus = com.project.sneakermarket.auction.entity.AuctionResultStatus.SOLD
              and not exists (select o.auctionOrderId from AuctionOrder o where o.auctionId = r.auctionId)
            order by r.auctionId
            """)
    List<Long> findOrderCandidateIds();
}
