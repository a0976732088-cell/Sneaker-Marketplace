package com.project.sneakermarket.auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.sneakermarket.auction.entity.AuctionCvsStoreSelection;

import jakarta.persistence.LockModeType;

public interface AuctionCvsStoreSelectionRepository extends JpaRepository<AuctionCvsStoreSelection, Long> {
    Optional<AuctionCvsStoreSelection> findByAuctionOrderId(Long auctionOrderId);
    boolean existsByMapToken(String mapToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select selection from AuctionCvsStoreSelection selection "
            + "where selection.auctionOrderId = :auctionOrderId")
    Optional<AuctionCvsStoreSelection> findByAuctionOrderIdForUpdate(
            @Param("auctionOrderId") Long auctionOrderId);
}
