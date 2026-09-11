package com.project.sneakermarket.advertisement.repository;

import com.project.sneakermarket.advertisement.entity.Advertisement;
import com.project.sneakermarket.advertisement.entity.AdvertisementPlacement;
import com.project.sneakermarket.advertisement.entity.AdvertisementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    List<Advertisement> findByAdStatusInAndEndAtBefore(
            List<AdvertisementStatus> adStatuses,
            LocalDateTime endAt
    );
    Page<Advertisement> findByAdStatus(
        AdvertisementStatus adStatus,
        Pageable pageable
    );
    List<Advertisement>
    findByAdStatusAndStartAtLessThanEqualAndEndAtAfterOrderBySortOrderAsc(
            AdvertisementStatus adStatus,
            LocalDateTime startAt,
            LocalDateTime endAt
    );
    long countByAdPlacementAndAdStatusAndStartAtLessThanEqualAndEndAtAfter(
            AdvertisementPlacement adPlacement,
            AdvertisementStatus adStatus,
            LocalDateTime startAt,
            LocalDateTime endAt
    );
    Page<Advertisement> findByAdStatusAndStartAtAfter(
            AdvertisementStatus adStatus,
            LocalDateTime startAt,
            Pageable pageable
    );

    Page<Advertisement> findByAdStatusAndStartAtLessThanEqualAndEndAtAfter(
            AdvertisementStatus adStatus,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Pageable pageable
    );
}
