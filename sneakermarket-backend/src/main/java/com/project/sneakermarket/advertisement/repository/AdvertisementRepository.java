package com.project.sneakermarket.advertisement.repository;

import com.project.sneakermarket.advertisement.entity.Advertisement;
import com.project.sneakermarket.advertisement.entity.AdvertisementPlacement;
import com.project.sneakermarket.advertisement.entity.AdvertisementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDateTime;
import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long>, JpaSpecificationExecutor<Advertisement> {

    List<Advertisement> findByAdStatusInAndEndAtBefore(
            List<AdvertisementStatus> adStatuses,
            LocalDateTime endAt
    );

    List<Advertisement>
    findByAdStatusAndStartAtLessThanEqualAndEndAtAfterOrderBySortOrderAsc(
            AdvertisementStatus adStatus,
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    /**
     * 查詢同一版位、投放時間重疊的廣告。
     * 重疊條件：既有廣告開始時間 < 新廣告結束時間且既有廣告結束時間 > 新廣告開始時間
     */
    @Query("""
            SELECT advertisement
            FROM Advertisement advertisement
            WHERE advertisement.adPlacement = :adPlacement
              AND advertisement.adStatus = :adStatus
              AND advertisement.adId <> :excludedAdId
              AND advertisement.startAt < :newEndAt
              AND advertisement.endAt > :newStartAt
            ORDER BY advertisement.startAt ASC
            """)
    List<Advertisement> findOverlappingAdvertisements(
            // 只檢查相同版位
            @Param("adPlacement")
            AdvertisementPlacement adPlacement,

            // 目前只讓 ACTIVE（包含已排程、進行中）占用時段
            @Param("adStatus")
            AdvertisementStatus adStatus,

            // 排除目前正在發布的廣告自己
            @Param("excludedAdId")
            Long excludedAdId,

            // 新廣告的開始時間
            @Param("newStartAt")
            LocalDateTime newStartAt,

            // 新廣告的結束時間
            @Param("newEndAt")
            LocalDateTime newEndAt
    );

    // jpa衍生寫法
    long countByAdStatusAndStartAtLessThanEqualAndEndAtAfter(
            AdvertisementStatus adStatus,
            LocalDateTime startAt,
            LocalDateTime endAt
    );
}
