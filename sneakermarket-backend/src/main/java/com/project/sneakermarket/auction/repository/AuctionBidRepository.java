package com.project.sneakermarket.auction.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.sneakermarket.auction.entity.AuctionBid;

/**
 * 競標出價紀錄 Repository。
 * 負責存取 dbo.auction_bids 資料表。
 */
public interface AuctionBidRepository
        extends JpaRepository<AuctionBid, Long> {

    @Query("select distinct b.bidderId from AuctionBid b where b.auctionId = :auctionId and b.bidderId <> :winnerId")
    List<Long> findDistinctLosingBidderIds(@Param("auctionId") Long auctionId, @Param("winnerId") Long winnerId);

    /** 先按競標聚合再由 DB 分頁；count 計算競標數而非出價筆數。 */
    @Query(value = """
            select b.auctionId as auctionId, max(b.bidAmount) as myHighestBid,
                   max(b.createdAt) as lastBidAt
            from AuctionBid b where b.bidderId = :memberId
            group by b.auctionId
            order by max(b.createdAt) desc, b.auctionId desc
            """, countQuery = """
            select count(distinct b.auctionId) from AuctionBid b where b.bidderId = :memberId
            """)
    Page<AuctionMemberBidSummary> findMemberBidSummaries(
            @Param("memberId") Long memberId, Pageable pageable);

    /**
     * 依競標 ID 查詢成功出價紀錄，
     * 並依出價時間由新到舊排列。
     *
     * @param auctionId 競標 ID
     * @return 該場競標的出價紀錄
     */
    List<AuctionBid> findByAuctionIdOrderByCreatedAtDesc(
            Long auctionId
    );

    /** 管理員詳情需在 DATETIME2(0) 同秒出價時仍維持確定的最新到最舊順序。 */
    List<AuctionBid> findByAuctionIdOrderByCreatedAtDescBidIdDesc(Long auctionId);
}
