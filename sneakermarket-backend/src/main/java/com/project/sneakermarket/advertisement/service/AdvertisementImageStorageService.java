package com.project.sneakermarket.advertisement.service;

import org.springframework.web.multipart.MultipartFile;

public interface AdvertisementImageStorageService {
    String store(MultipartFile file);

    void delete (String imageUrl);
}
