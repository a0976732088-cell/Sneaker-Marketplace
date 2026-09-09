package com.project.sneakermarket.advertisement.service;

import com.project.sneakermarket.advertisement.dto.AdvertisementDeliveryStatus;
import com.project.sneakermarket.advertisement.dto.AdvertisementResponse;
import com.project.sneakermarket.advertisement.dto.CreateAdvertisementRequest;
import com.project.sneakermarket.advertisement.dto.UpdateAdvertisementRequest;
import com.project.sneakermarket.advertisement.dto.UpdateAdvertisementStatusRequest;
import com.project.sneakermarket.advertisement.entity.Advertisement;
import com.project.sneakermarket.advertisement.entity.AdvertisementPlacement;
import com.project.sneakermarket.advertisement.entity.AdvertisementStatus;
import com.project.sneakermarket.advertisement.repository.AdvertisementRepository;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private final AdvertisementRepository adRepo;

    public Advertisement insertAdvertisement(CreateAdvertisementRequest request) {
        Advertisement advertisement = new Advertisement();

        validateAdvertisementTime(request.getStartAt(), request.getEndAt());
        validateDraftStartAtNotInPast(request.getStartAt());
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

    public List<AdvertisementResponse> getPublicActiveAdvertisements(){
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

        if (advertisement.getAdStatus() ==AdvertisementStatus.DRAFT){
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

    public Page<AdvertisementResponse> findAdByPage(Integer pageNumber,
                                                    AdvertisementStatus adStatus,
                                                    AdvertisementDeliveryStatus deliveryStatus){
        Pageable page = PageRequest.of(pageNumber,5, Sort.Direction.DESC, "createdAt");

        Page<Advertisement> result;

        LocalDateTime now =
                LocalDateTime.now(ZoneId.of("Asia/Taipei"));

        if (deliveryStatus == AdvertisementDeliveryStatus.SCHEDULED) {
            result = adRepo.findByAdStatusAndStartAtAfter(
                    AdvertisementStatus.ACTIVE,
                    now,
                    page
            );
        } else if (deliveryStatus == AdvertisementDeliveryStatus.RUNNING) {
            result =
                    adRepo.findByAdStatusAndStartAtLessThanEqualAndEndAtAfter(
                            AdvertisementStatus.ACTIVE,
                            now,
                            now,
                            page
                    );
        } else if (adStatus != null) {
            result = adRepo.findByAdStatus(adStatus, page);
        } else {
            result = adRepo.findAll(page);
        }

        return result.map(this::toResponse);
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

    private void validateDraftStartAtNotInPast(LocalDateTime startAt){
        if (startAt == null){
            return;
        }

        LocalDateTime currentMinute = LocalDateTime.now(ZoneId.of("Asia/Taipei"))
                .withSecond(0)
                .withNano(0);
        if (startAt.isBefore(currentMinute)){
            throw new BusinessException("開始時間不可早於目前分鐘");
        }
    }

    private void validateStatusTransition(
            Advertisement ad,
            AdvertisementStatus newStatus
    ){
        AdvertisementStatus currentStatus = ad.getAdStatus();

        boolean validTransition = (currentStatus == AdvertisementStatus.DRAFT
                && newStatus == AdvertisementStatus.ACTIVE)
                || (currentStatus == AdvertisementStatus.ACTIVE
                && newStatus == AdvertisementStatus.INACTIVE)
                || (currentStatus == AdvertisementStatus.INACTIVE
                && newStatus == AdvertisementStatus.ACTIVE);

        if (!validTransition){
            throw new BusinessException("不允許此廣告狀態轉換");
        }

        if(newStatus == AdvertisementStatus.ACTIVE){
            validateAdvertisementCanBeActivated(ad);
            validatePlacementAvailability(ad);

        }
    }

    private void validateAdvertisementCanBeActivated(Advertisement ad){
        if (ad.getStartAt() == null || ad.getEndAt() == null){
            throw new BusinessException("上架前必須設定開始與結束時間");
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Taipei"));

        if (!now.isBefore(ad.getEndAt())) {
            throw new BusinessException("廣告已超過結束時間");
        }
    }

    private void validatePlacementAvailability(Advertisement advertisement) {
        AdvertisementPlacement placement = advertisement.getAdPlacement();

        if (placement == AdvertisementPlacement.BANNER) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Taipei"));

        long activeAdvertisementCount =
                adRepo.countByAdPlacementAndAdStatusAndStartAtLessThanEqualAndEndAtAfter(
                        placement,
                        AdvertisementStatus.ACTIVE,
                        now,
                        now
                );

        if (activeAdvertisementCount > 0) {
            throw new BusinessException(
                    placement == AdvertisementPlacement.POPUP
                            ? "目前已有正在投放的 POPUP 廣告"
                            : "目前已有正在投放的 SMALL_BLOCK 廣告"
            );
        }
    }

    @Transactional
    public AdvertisementResponse updateAdvertisementStatus(Long adId, UpdateAdvertisementStatusRequest request){
        Advertisement ad = adRepo.findById(adId)
                .orElseThrow(()->
                        new ResourceNotFoundException("找不到指定的廣告"));

        ensureAdvertisementIsNotExpired(ad);

        AdvertisementStatus newStatus = request.getAdStatus();

        validateStatusTransition(ad, newStatus);
        ad.setAdStatus(newStatus);

        Advertisement saveAd = adRepo.save(ad);

        return toResponse(saveAd);
    }


}