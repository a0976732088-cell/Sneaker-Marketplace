package com.project.sneakermarket.advertisement.service;

import com.project.sneakermarket.advertisement.dto.CreateAdvertisementRequest;
import com.project.sneakermarket.advertisement.entity.Advertisement;
import com.project.sneakermarket.advertisement.entity.AdvertisementStatus;
import com.project.sneakermarket.advertisement.repository.AdvertisementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private final AdvertisementRepository adRepo;

    public Advertisement insertAdvertisement(CreateAdvertisementRequest request){
        Advertisement advertisement = new Advertisement();

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
}
