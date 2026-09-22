package com.project.sneakermarket.auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.sneakermarket.auction.entity.AuctionLinePayTransaction;

import jakarta.persistence.LockModeType;

public interface AuctionLinePayTransactionRepository extends JpaRepository<AuctionLinePayTransaction, Long> {
    Optional<AuctionLinePayTransaction> findByMerchantOrderId(String merchantOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tx from AuctionLinePayTransaction tx where tx.merchantOrderId = :merchantOrderId")
    Optional<AuctionLinePayTransaction> findByMerchantOrderIdForUpdate(
            @Param("merchantOrderId") String merchantOrderId);

    boolean existsByMerchantOrderId(String merchantOrderId);
    boolean existsByLinePayTransactionId(String linePayTransactionId);
}
