package com.project.sneakermarket.auction.repository;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.sneakermarket.auction.entity.Auction;

import jakarta.persistence.LockModeType;

/**
 * 競標主資料 Repository。
 * 負責存取 dbo.auctions 資料表。
 *
 * JpaRepository 已提供基本 CRUD 功能，
 * JpaSpecificationExecutor 則提供動態條件查詢能力。
 */
public interface AuctionRepository
        extends JpaRepository<Auction, Long>,
        JpaSpecificationExecutor<Auction> {

    /**
     * 以資料庫悲觀寫入鎖查詢指定競標。
     *
     * 用於編輯、取消、出價等會改變競標狀態的操作，
     * 避免多個交易同時依舊資料進行判斷。
     *
     * 此方法應在 @Transactional Service 中呼叫，
     * Transaction 結束後鎖才會釋放。
     *
     * @param auctionId 競標 ID
     * @return 被鎖定的競標資料
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
            from Auction a
            where a.auctionId = :auctionId
            """)
    Optional<Auction> findByIdForUpdate(
            @Param("auctionId") Long auctionId
    );

    /** 僅投影到期候選 ID；Service 取得 row lock 後仍須重新判斷。 */
    @Query("""
            select a.auctionId from Auction a
            where (a.status = com.project.sneakermarket.auction.entity.AuctionStatus.SCHEDULED
                   and a.startAt <= :now)
               or (a.status = com.project.sneakermarket.auction.entity.AuctionStatus.ACTIVE
                   and a.endAt <= :now)
            order by a.auctionId
            """)
    List<Long> findLifecycleCandidateIds(@Param("now") LocalDateTime now);
}
