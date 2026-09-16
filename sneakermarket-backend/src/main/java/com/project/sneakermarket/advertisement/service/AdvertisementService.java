package com.project.sneakermarket.advertisement.service;

import com.project.sneakermarket.advertisement.dto.AdvertisementDeliveryStatus;
import com.project.sneakermarket.advertisement.dto.AdvertisementResponse;
import com.project.sneakermarket.advertisement.dto.CreateAdvertisementRequest;
import com.project.sneakermarket.advertisement.dto.UpdateAdvertisementRequest;
import com.project.sneakermarket.advertisement.dto.UpdateAdvertisementStatusRequest;
import com.project.sneakermarket.advertisement.entity.Advertisement;
import com.project.sneakermarket.advertisement.entity.AdvertisementPlacement;
import com.project.sneakermarket.advertisement.entity.AdvertisementStatus;
import com.project.sneakermarket.advertisement.entity.AdvertisementTargetType;
import com.project.sneakermarket.advertisement.repository.AdvertisementRepository;
import com.project.sneakermarket.commerce.repository.CategoryRepository;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;


import java.util.ArrayList;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdvertisementService {


    private final AdvertisementRepository adRepo;

    // 用來讀取及驗證已上傳的廣告圖片
    private final AdvertisementImageStorageService adImageService;

    private final CategoryRepository categoryRepo;


    public Advertisement insertAdvertisement(CreateAdvertisementRequest request) {
        Advertisement advertisement = new Advertisement();

        validateAdvertisementTime(request.getStartAt(), request.getEndAt());
        validateDraftStartAtNotInPast(request.getStartAt());
        // 根據目前選擇的版位，重新驗證伺服器裡的圖片
        adImageService.validateStoredImage(
                request.getImageUrl(),
                request.getAdPlacement()
        );
        validateAdvertisementTarget(request.getTargetType(), request.getTargetId());

        advertisement.setCreatedBy(request.getCreatedBy());
        advertisement.setAdName(request.getAdName());
        advertisement.setImageUrl(request.getImageUrl());
        advertisement.setAdDescription(request.getAdDescription());
        advertisement.setAdType(request.getAdType());
        advertisement.setAdPlacement(request.getAdPlacement());
        advertisement.setStartAt(request.getStartAt());
        advertisement.setEndAt(request.getEndAt());
        advertisement.setTargetType(request.getTargetType());
        advertisement.setTargetId(request.getTargetId());
        advertisement.setTargetUrl(request.getTargetUrl());

        advertisement.setSortOrder(
                request.getSortOrder() == null ? 0 : request.getSortOrder()
        );

        advertisement.setAdStatus(AdvertisementStatus.DRAFT);


        return adRepo.save(advertisement);
    }

    public List<AdvertisementResponse> getAllAdvertisements() {
        return adRepo.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AdvertisementResponse> getPublicActiveAdvertisements() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Taipei"));

        return adRepo
                .findByAdStatusAndStartAtLessThanEqualAndEndAtAfterOrderBySortOrderAsc(
                        AdvertisementStatus.ACTIVE,
                        now,
                        now
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AdvertisementResponse toResponse(Advertisement advertisement) {
        AdvertisementResponse response = new AdvertisementResponse();

        response.setAdId(advertisement.getAdId());
        response.setAdName(advertisement.getAdName());
        response.setImageUrl(advertisement.getImageUrl());
        response.setAdDescription(advertisement.getAdDescription());
        response.setAdType(advertisement.getAdType());
        response.setAdPlacement(advertisement.getAdPlacement());
        response.setStartAt(advertisement.getStartAt());
        response.setEndAt(advertisement.getEndAt());
        response.setTargetType(advertisement.getTargetType());
        response.setTargetId(advertisement.getTargetId());
        response.setTargetUrl(advertisement.getTargetUrl());
        response.setSortOrder(advertisement.getSortOrder());
        response.setAdStatus(advertisement.getAdStatus());
        response.setCreatedAt(advertisement.getCreatedAt());
        response.setUpdatedAt(advertisement.getUpdatedAt());

        return response;
    }

    //未來加入member="admin"需要修改
    public void deleteAdvertisement(Long adId) {

        Advertisement advertisement = adRepo.findById(adId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("找不到指定的廣告")
                );

        ensureAdvertisementCanBeDeleted(advertisement);

        adRepo.delete(advertisement);
    }

    private void ensureAdvertisementCanBeDeleted(
            Advertisement advertisement
    ) {
        AdvertisementStatus status =
                advertisement.getAdStatus();

        if (status != AdvertisementStatus.DRAFT
                && status != AdvertisementStatus.INACTIVE) {
            throw new BusinessException(
                    "只有草稿、已取消排程或已暫停的廣告可以刪除"
            );
        }
    }

    private void ensureAdvertisementIsNotExpired(Advertisement advertisement) {
        if (advertisement.getAdStatus() == AdvertisementStatus.EXPIRED) {
            throw new BusinessException("已結束的廣告不可修改、刪除或變更狀態");
        }
    }

    public Advertisement updateAdvertisement(Long adId, UpdateAdvertisementRequest request) {
        Advertisement advertisement = adRepo.findById(adId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("找不到指定的廣告")
                );

        ensureAdvertisementIsEditable(advertisement);

        validateAdvertisementTime(request.getStartAt(), request.getEndAt());
        // 根據目前選擇的版位，重新驗證伺服器裡的圖片
        adImageService.validateStoredImage(
                request.getImageUrl(),
                request.getAdPlacement()
        );
        validateAdvertisementTarget(request.getTargetType(), request.getTargetId());

        if (advertisement.getAdStatus() == AdvertisementStatus.DRAFT) {
            validateDraftStartAtNotInPast(request.getStartAt());
        }

        boolean startAtChanged = !Objects.equals(
                advertisement.getStartAt(),
                request.getStartAt()
        );

        if (advertisement.getAdStatus() == AdvertisementStatus.INACTIVE
                && startAtChanged) {

            LocalDateTime now =
                    LocalDateTime.now(ZoneId.of("Asia/Taipei"));

            LocalDateTime originalStartAt =
                    advertisement.getStartAt();

            // 原本的開始時間已經到達，代表廣告曾經開始投放。
            if (originalStartAt != null
                    && !now.isBefore(originalStartAt)) {
                throw new BusinessException(
                        "已開始投放的廣告不可修改開始時間"
                );
            }

            // 尚未開始的已取消排程廣告，可以重新安排未來時間。
            validateDraftStartAtNotInPast(request.getStartAt());
        }

        advertisement.setAdName(request.getAdName());
        advertisement.setImageUrl(request.getImageUrl());
        advertisement.setAdDescription(request.getAdDescription());
        advertisement.setAdType(request.getAdType());
        advertisement.setAdPlacement(request.getAdPlacement());
        advertisement.setStartAt(request.getStartAt());
        advertisement.setEndAt(request.getEndAt());
        advertisement.setTargetType(request.getTargetType());
        advertisement.setTargetId(request.getTargetId());
        advertisement.setTargetUrl(request.getTargetUrl());

        advertisement.setSortOrder((request.getSortOrder() == null ? 0 : request.getSortOrder()));


        return adRepo.save(advertisement);
    }

    public Page<AdvertisementResponse> findAdByPage(
            Integer pageNumber,
            AdvertisementStatus adStatus,
            AdvertisementDeliveryStatus deliveryStatus,
            AdvertisementPlacement adPlacement,
            String keyword
    ) {
        Pageable page = PageRequest.of(
                pageNumber,
                5,
                Sort.Direction.DESC,
                "createdAt"
        );

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Taipei"));

        Specification<Advertisement> specification =
                (root, query, criteriaBuilder) -> {

                    List<Predicate> predicates = new ArrayList<>();

                    // 版位篩選
                    if (adPlacement != null) {
                        predicates.add(
                                criteriaBuilder.equal(
                                        root.<AdvertisementPlacement>get("adPlacement"),
                                        adPlacement
                                )
                        );
                    }

                    // 投放狀態篩選
                    if (deliveryStatus == AdvertisementDeliveryStatus.SCHEDULED) {
                        predicates.add(
                                criteriaBuilder.equal(
                                        root.<AdvertisementStatus>get("adStatus"),
                                        AdvertisementStatus.ACTIVE
                                )
                        );
                        predicates.add(
                                criteriaBuilder.greaterThan(
                                        root.<LocalDateTime>get("startAt"),
                                        now
                                )
                        );

                    } else if (deliveryStatus == AdvertisementDeliveryStatus.RUNNING) {
                        predicates.add(
                                criteriaBuilder.equal(
                                        root.<AdvertisementStatus>get("adStatus"),
                                        AdvertisementStatus.ACTIVE
                                )
                        );
                        predicates.add(
                                criteriaBuilder.lessThanOrEqualTo(
                                        root.<LocalDateTime>get("startAt"),
                                        now
                                )
                        );
                        predicates.add(
                                criteriaBuilder.greaterThan(
                                        root.<LocalDateTime>get("endAt"),
                                        now
                                )
                        );

                    } else if (adStatus != null) {
                        predicates.add(
                                criteriaBuilder.equal(
                                        root.<AdvertisementStatus>get("adStatus"),
                                        adStatus
                                )
                        );
                    }

                    // 關鍵字：搜尋廣告名稱，或精確搜尋廣告編號
                    String normalizedKeyword = keyword == null
                            ? ""
                            : keyword.trim().toLowerCase(Locale.ROOT);

                    if (!normalizedKeyword.isEmpty()) {
                        List<Predicate> keywordPredicates = new ArrayList<>();

                        keywordPredicates.add(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.<String>get("adName")
                                        ),
                                        "%" + normalizedKeyword + "%"
                                )
                        );

                        try {
                            Long adId = Long.valueOf(normalizedKeyword);

                            keywordPredicates.add(
                                    criteriaBuilder.equal(
                                            root.<Long>get("adId"),
                                            adId
                                    )
                            );
                        } catch (NumberFormatException ignored) {
                            // 非數字關鍵字只搜尋廣告名稱。
                        }

                        predicates.add(
                                criteriaBuilder.or(
                                        keywordPredicates.toArray(new Predicate[0])
                                )
                        );
                    }

                    return criteriaBuilder.and(
                            predicates.toArray(new Predicate[0])
                    );
                };

        return adRepo.findAll(specification, page)
                .map(this::toResponse);
    }

    @Transactional
    public int expireEndedAdvertisements() {
        LocalDateTime now =
                LocalDateTime.now(ZoneId.of("Asia/Taipei"));

        List<Advertisement> expiredAds = adRepo.findByAdStatusInAndEndAtBefore(
                List.of(
                        AdvertisementStatus.ACTIVE,
                        AdvertisementStatus.INACTIVE
                ), now);

        expiredAds.forEach(advertisement ->
                advertisement.setAdStatus(AdvertisementStatus.EXPIRED)
        );

        adRepo.saveAll(expiredAds);

        return expiredAds.size();
    }


    private void ensureAdvertisementIsEditable(Advertisement advertisement) {
        if (advertisement.getAdStatus() != AdvertisementStatus.DRAFT
                && advertisement.getAdStatus() != AdvertisementStatus.INACTIVE) {
            throw new BusinessException("只有草稿或已停用的廣告可以編輯");
        }
    }

    private void validateAdvertisementTime(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        if (startAt != null
                && endAt != null
                && !startAt.isBefore(endAt)) {
            throw new BusinessException("開始時間必須早於結束時間");
        }
    }

    private void validateDraftStartAtNotInPast(LocalDateTime startAt) {
        if (startAt == null) {
            return;
        }

        LocalDateTime currentMinute = LocalDateTime.now(ZoneId.of("Asia/Taipei"))
                .withSecond(0)
                .withNano(0);
        if (startAt.isBefore(currentMinute)) {
            throw new BusinessException("開始時間不可早於目前分鐘");
        }
    }

    private void validateStatusTransition(
            Advertisement ad,
            AdvertisementStatus newStatus
    ) {
        AdvertisementStatus currentStatus = ad.getAdStatus();

        boolean validTransition = (currentStatus == AdvertisementStatus.DRAFT
                && newStatus == AdvertisementStatus.ACTIVE)
                || (currentStatus == AdvertisementStatus.ACTIVE
                && newStatus == AdvertisementStatus.INACTIVE)
                || (currentStatus == AdvertisementStatus.INACTIVE
                && newStatus == AdvertisementStatus.ACTIVE);

        if (!validTransition) {
            throw new BusinessException("不允許此廣告狀態轉換");
        }

        if (newStatus == AdvertisementStatus.ACTIVE) {
            validateAdvertisementCanBeActivated(ad);
            validatePlacementAvailability(ad);

        }
    }

    private void validateAdvertisementCanBeActivated(Advertisement ad) {
        if (ad.getStartAt() == null || ad.getEndAt() == null) {
            throw new BusinessException("上架前必須設定開始與結束時間");
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Taipei"));

        if (!now.isBefore(ad.getEndAt())) {
            throw new BusinessException("廣告已超過結束時間");
        }
    }

    /**
     * 檢查廣告版位的投放時段是否可以使用。
     * BANNER、CATEGORY_CARD 可以同時存在多則廣告，因為前端會輪播。
     * POPUP、SMALL_BLOCK 同一時段只能有一則。
     */
    private void validatePlacementAvailability(
            Advertisement advertisement
    ) {
        AdvertisementPlacement placement =
                advertisement.getAdPlacement();

        // BANNER 可以同時投放多則，不需要檢查時段重疊
        if (placement == AdvertisementPlacement.BANNER || placement == AdvertisementPlacement.CATEGORY_CARD) {
            return;
        }

        /*
         * 查詢同一版位、狀態為 ACTIVE，
         * 而且投放時間與目前廣告重疊的資料。
         *
         * 目前的 ACTIVE 同時包含：
         * 1. 已排程但尚未開始
         * 2. 正在投放
         */
        List<Advertisement> overlappingAdvertisements =
                adRepo.findOverlappingAdvertisements(
                        placement,
                        AdvertisementStatus.ACTIVE,
                        advertisement.getAdId(),
                        advertisement.getStartAt(),
                        advertisement.getEndAt()
                );

        // 沒有查到資料，代表這個時段可以使用
        if (overlappingAdvertisements.isEmpty()) {
            return;
        }

        // 取出第一則衝突的廣告，讓錯誤訊息更清楚
        Advertisement conflictingAdvertisement =
                overlappingAdvertisements.get(0);

        throw new BusinessException(
                placement == AdvertisementPlacement.POPUP
                        ? "此 POPUP 投放時段與廣告「"
                        + conflictingAdvertisement.getAdName()
                        + "」重疊，請調整時間"
                        : "此 SMALL_BLOCK 投放時段與廣告「"
                        + conflictingAdvertisement.getAdName()
                        + "」重疊，請調整時間"
        );
    }

    @Transactional
    public AdvertisementResponse updateAdvertisementStatus(Long adId, UpdateAdvertisementStatusRequest request) {
        Advertisement ad = adRepo.findById(adId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("找不到指定的廣告"));

        ensureAdvertisementIsNotExpired(ad);

        AdvertisementStatus newStatus = request.getAdStatus();

        validateStatusTransition(ad, newStatus);
        ad.setAdStatus(newStatus);

        Advertisement saveAd = adRepo.save(ad);

        return toResponse(saveAd);
    }

    private void validateAdvertisementTarget(
            AdvertisementTargetType targetType,
            Long targetId
    ) {
        if (targetType != AdvertisementTargetType.CATEGORY) {
            return;
        }

        if (targetId == null){
            throw new BusinessException("分類導向必須選擇商品分類");
        }

        if(!categoryRepo.existsById(targetId)){
            throw new BusinessException("找不到指定的商品分類");
        }
    }
}