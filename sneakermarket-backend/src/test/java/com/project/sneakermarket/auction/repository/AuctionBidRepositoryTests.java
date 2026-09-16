package com.project.sneakermarket.auction.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionBid;

/**
 * AuctionBidRepository 整合測試。
 *
 * 用來確認 Spring Data JPA 是否能正常存取
 * dbo.auction_bids，並依競標 ID 查詢成功出價紀錄。
 */
@SpringBootTest
@Transactional
class AuctionBidRepositoryTests {

    // 注入出價紀錄 Repository
    @Autowired
    private AuctionBidRepository auctionBidRepository;

    // 注入既有競標 Repository，取得實際存在的 auctionId
    @Autowired
    private AuctionRepository auctionRepository;

    /**
     * 測試出價紀錄是否可以正常新增，
     * 並依 createdAt 由新到舊查詢。
     */
    @Test
    void findByAuctionIdOrderByCreatedAtDesc_ShouldReturnBidsInOrder() {

        // 取得資料庫中既有的一場競標，
        // 避免將 auctionId 寫死為固定數字。
        Auction auction = auctionRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("目前沒有競標測試資料"));

        // 固定測試基準時間，並移除奈秒，
        // 與 MSSQL DATETIME2(0) 的秒級精度保持一致。
        LocalDateTime baseTime = LocalDateTime.now().withNano(0);

        // 建立較早的一筆成功出價
        AuctionBid olderBid = new AuctionBid();
        olderBid.setAuctionId(auction.getAuctionId());
        olderBid.setBidderId(90001L);
        olderBid.setBidAmount(new BigDecimal("5000.00"));
        olderBid.setCreatedAt(baseTime.minusMinutes(2));

        // 建立較新的一筆成功出價
        AuctionBid newerBid = new AuctionBid();
        newerBid.setAuctionId(auction.getAuctionId());
        newerBid.setBidderId(90002L);
        newerBid.setBidAmount(new BigDecimal("5500.00"));
        newerBid.setCreatedAt(baseTime.minusMinutes(1));

        // 寫入測試資料
        auctionBidRepository.saveAndFlush(olderBid);
        auctionBidRepository.saveAndFlush(newerBid);

        // Identity 主鍵應由資料庫正常產生
        assertNotNull(olderBid.getBidId());
        assertNotNull(newerBid.getBidId());

        // 查詢此場競標的所有成功出價紀錄
        List<AuctionBid> bids =
                auctionBidRepository
                        .findByAuctionIdOrderByCreatedAtDesc(
                                auction.getAuctionId()
                        );

        // 找出剛建立兩筆資料的位置
        int newerIndex = findBidIndex(bids, newerBid.getBidId());
        int olderIndex = findBidIndex(bids, olderBid.getBidId());

        // 兩筆資料都必須查得到
        assertTrue(newerIndex >= 0);
        assertTrue(olderIndex >= 0);

        // 較新的出價應排在較舊的出價前面
        assertTrue(newerIndex < olderIndex);

        // 確認 BigDecimal 金額映射正常
        assertEquals(
                0,
                new BigDecimal("5500.00")
                        .compareTo(newerBid.getBidAmount())
        );

        // 印出結果，方便開發階段人工確認
        bids.forEach(bid -> {
            System.out.println(
                    "出價 ID：" + bid.getBidId()
                            + "，競標 ID：" + bid.getAuctionId()
                            + "，會員 ID：" + bid.getBidderId()
                            + "，金額：" + bid.getBidAmount()
                            + "，時間：" + bid.getCreatedAt()
            );
        });
    }

    @Test
    void findDistinctBidderIdsReturnsEachParticipantOnce() {
        Auction auction = auctionRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("目前沒有競標測試資料"));
        LocalDateTime baseTime = LocalDateTime.now().withNano(0);
        Long repeatedBidderId = 91001L;
        Long otherBidderId = 91002L;

        auctionBidRepository.saveAndFlush(bid(
                auction.getAuctionId(), repeatedBidderId, "6000.00", baseTime.minusSeconds(3)));
        auctionBidRepository.saveAndFlush(bid(
                auction.getAuctionId(), repeatedBidderId, "6100.00", baseTime.minusSeconds(2)));
        auctionBidRepository.saveAndFlush(bid(
                auction.getAuctionId(), otherBidderId, "6200.00", baseTime.minusSeconds(1)));

        List<Long> bidderIds = auctionBidRepository.findDistinctBidderIds(auction.getAuctionId());

        assertEquals(1L, bidderIds.stream().filter(repeatedBidderId::equals).count());
        assertEquals(1L, bidderIds.stream().filter(otherBidderId::equals).count());
    }

    private AuctionBid bid(Long auctionId, Long bidderId, String amount, LocalDateTime createdAt) {
        AuctionBid bid = new AuctionBid();
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setBidAmount(new BigDecimal(amount));
        bid.setCreatedAt(createdAt);
        return bid;
    }

    /**
     * 依 bidId 找出指定出價紀錄在查詢結果中的位置。
     *
     * @param bids  出價紀錄清單
     * @param bidId 出價 ID
     * @return 所在位置；找不到時回傳 -1
     */
    private int findBidIndex(
            List<AuctionBid> bids,
            Long bidId
    ) {

        for (int i = 0; i < bids.size(); i++) {
            if (bids.get(i).getBidId().equals(bidId)) {
                return i;
            }
        }

        return -1;
    }
}
