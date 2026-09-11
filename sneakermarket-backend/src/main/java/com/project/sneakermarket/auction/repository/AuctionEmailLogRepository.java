package com.project.sneakermarket.auction.repository;

import java.util.List;
import java.util.Optional;
import com.project.sneakermarket.auction.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AuctionEmailLogRepository extends JpaRepository<AuctionEmailLog, Long> {
    List<AuctionEmailLog> findByAuctionId(Long auctionId);
    boolean existsByAuctionIdAndRecipientMemberIdAndEmailType(Long auctionId, Long memberId, AuctionEmailType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from AuctionEmailLog e where e.emailLogId = :id")
    Optional<AuctionEmailLog> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select e.emailLogId from AuctionEmailLog e
            where (:auctionId is null or e.auctionId = :auctionId)
              and e.sendStatus in (com.project.sneakermarket.auction.entity.AuctionEmailSendStatus.PENDING,
                                   com.project.sneakermarket.auction.entity.AuctionEmailSendStatus.FAILED)
              and e.retryCount < :maxRetries
            order by e.createdAt, e.emailLogId
            """)
    List<Long> findDispatchCandidateIds(@Param("auctionId") Long auctionId, @Param("maxRetries") int maxRetries);
}
