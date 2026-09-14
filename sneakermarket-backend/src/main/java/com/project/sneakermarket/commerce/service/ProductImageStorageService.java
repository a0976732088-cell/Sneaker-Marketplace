package com.project.sneakermarket.commerce.service;

import org.springframework.web.multipart.MultipartFile;

public interface ProductImageStorageService {
    String store(MultipartFile file);
    void delete(String imageUrl);
}