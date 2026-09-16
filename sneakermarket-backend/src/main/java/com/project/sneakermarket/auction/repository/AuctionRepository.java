package com.project.sneakermarket.auction.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.sneakermarket.auction.entity.Auction;

import jakarta.persistence.LockModeType;

/** 競標主資料 Repository；管理寫入一律透過 findByIdForUpdate 競爭同一筆 row lock。 */
public interface AuctionRepository extends JpaRepository<Auction, Long>, JpaSpecificationExecutor<Auction> {

    /** 歷史刊登保留下架與刪除資料，讓賣家仍能查看自己的交易歷史。 */
    @Query("""
            select a from Auction a where a.auctionProductId in
                (select p.auctionProductId from AuctionProduct p where p.sellerId = :memberId)
            """)
    Page<Auction> findMemberListings(@Param("memberId") Long memberId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.auctionId = :auctionId")
    Optional<Auction> findByIdForUpdate(@Param("auctionId") Long auctionId);

    /** 公開端點共同使用；保留的 DRAFT 與舊資料 REMOVED status 均不可曝光。 */
    @Query("""
            select a from Auction a
            where a.auctionId = :auctionId
              and a.removedAt is null
              and a.deletedAt is null
              and a.status <> com.project.sneakermarket.auction.entity.AuctionStatus.REMOVED
              and a.status <> com.project.sneakermarket.auction.entity.AuctionStatus.DRAFT
            """)
    Optional<Auction> findPublicById(@Param("auctionId") Long auctionId);

    /** 僅投影可處理的候選 ID；Service 取得 row lock 後仍會重新判斷 metadata 與狀態。 */
    @Query("""
            select a.auctionId from Auction a
            where a.deletedAt is null
              and ((a.status = com.project.sneakermarket.auction.entity.AuctionStatus.SCHEDULED
                    and ((a.removedAt is null and a.startAt <= :now)
                      or (a.removedAt is not null and a.endAt <= :now)))
                or (a.status = com.project.sneakermarket.auction.entity.AuctionStatus.ACTIVE
                    and a.endAt <= :now))
            order by a.auctionId
            """)
    List<Long> findLifecycleCandidateIds(@Param("now") LocalDateTime now);
}
