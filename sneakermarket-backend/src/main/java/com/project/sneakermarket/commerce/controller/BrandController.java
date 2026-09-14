package com.project.sneakermarket.commerce.controller;

import com.project.sneakermarket.commerce.dto.BrandResponse;
import com.project.sneakermarket.commerce.entity.Brand;
import com.project.sneakermarket.commerce.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class BrandController {

    private final BrandRepository brandRepository;

    //回傳品牌名稱清單
    @GetMapping("/api/brands")
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll().stream()
                .sorted(Comparator.comparing(Brand::getDisplayOrder))
                .map(b -> new BrandResponse(b.getBrandId(), b.getBrandName()))
                .collect(Collectors.toList());
    }
}
