package com.project.sneakermarket.advertisement.service;

import org.springframework.web.multipart.MultipartFile;

import com.project.sneakermarket.advertisement.entity.AdvertisementPlacement;

public interface AdvertisementImageStorageService {
    String store(MultipartFile file, AdvertisementPlacement adPlacement);

    void validateStoredImage(String imageUrl, AdvertisementPlacement adPlacement);

    void delete (String imageUrl);
}
