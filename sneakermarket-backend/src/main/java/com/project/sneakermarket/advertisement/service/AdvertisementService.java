package com.project.sneakermarket.advertisement.service;

import com.project.sneakermarket.advertisement.dto.AdvertisementResponse;
import com.project.sneakermarket.advertisement.dto.CreateAdvertisementRequest;
import com.project.sneakermarket.advertisement.dto.UpdateAdvertisementRequest;
import com.project.sneakermarket.advertisement.entity.Advertisement;
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

@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private final AdvertisementRepository adRepo;

    public Advertisement insertAdvertisement(CreateAdvertisementRequest request) {
        Advertisement advertisement = new Advertisement();

        validateAdvertisementTime(request.getStartAt(), request.getEndAt());
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
        adRepo.deleteById(adId);
    }

    public Advertisement updateAdvertisement(Long adId, UpdateAdvertisementRequest request) {
        Advertisement advertisement = adRepo.findById(adId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("找不到指定的廣告")
                );
        validateAdvertisementTime(request.getStartAt(), request.getEndAt());

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

    public Page<AdvertisementResponse> findAdByPage(Integer pageNumber){
        Pageable page = PageRequest.of(pageNumber,5, Sort.Direction.DESC, "createdAt");
        return adRepo.findAll(page)
                .map(this::toResponse);
    }

    @Transactional
    public int expireEndedAdvertisements() {
        LocalDateTime now =
                LocalDateTime.now(ZoneId.of("Asia/Taipei"));

        List<Advertisement> expiredAds = adRepo.findByAdStatusAndEndAtBefore(AdvertisementStatus.ACTIVE, now);

        expiredAds.forEach(advertisement ->
                advertisement.setAdStatus(AdvertisementStatus.EXPIRED)
        );

        adRepo.saveAll(expiredAds);

        return expiredAds.size();
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
}