package com.project.sneakermarket.advertisement.controller;

import com.project.sneakermarket.advertisement.dto.AdvertisementResponse;
import com.project.sneakermarket.advertisement.dto.CreateAdvertisementRequest;
import com.project.sneakermarket.advertisement.entity.Advertisement;
import com.project.sneakermarket.advertisement.service.AdvertisementService;
import com.project.sneakermarket.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ads")
@RequiredArgsConstructor
public class AdvertisementController {

    private final AdvertisementService adService;
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Long>> insertAdvertisement(@Valid @RequestBody CreateAdvertisementRequest request) {

        Advertisement advertisement = adService.insertAdvertisement(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("廣告新增成功，尚未上架", advertisement.getAdId()));
    }

    @GetMapping("findAll")
    public ResponseEntity<ApiResponse<List<AdvertisementResponse>>> getAllAdvertisements() {
        List<AdvertisementResponse> advertisements = adService.getAllAdvertisements();

        return ResponseEntity.ok(
                ApiResponse.ok("廣告列表查詢成功", advertisements)
        );
    }

}
