package com.project.sneakermarket.advertisement.controller;

import com.project.sneakermarket.advertisement.dto.AdvertisementResponse;
import com.project.sneakermarket.advertisement.dto.CreateAdvertisementRequest;
import com.project.sneakermarket.advertisement.dto.UpdateAdvertisementRequest;
import com.project.sneakermarket.advertisement.entity.Advertisement;
import com.project.sneakermarket.advertisement.service.AdvertisementService;
import com.project.sneakermarket.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    @GetMapping("/findAll")
    public ResponseEntity<ApiResponse<List<AdvertisementResponse>>> getAllAdvertisements() {
        List<AdvertisementResponse> advertisements = adService.getAllAdvertisements();

        return ResponseEntity.ok(
                ApiResponse.ok("廣告列表查詢成功", advertisements)
        );
    }

    @DeleteMapping("/delete/{adId}")
    public ResponseEntity<ApiResponse<Void>> deleteAdvertisement(@PathVariable Long adId) {
        adService.deleteAdvertisement(adId);

        return ResponseEntity.ok(ApiResponse.ok("廣告刪除成功",null));

    }

    @PutMapping("/put/{adId}")
    public ResponseEntity<ApiResponse<Long>> updateAdvertisement(
                @PathVariable Long adId,
                @Valid @RequestBody UpdateAdvertisementRequest request){
            Advertisement advertisement = adService.updateAdvertisement(adId,request);

            return ResponseEntity.ok(ApiResponse.ok("廣告修改成功", advertisement.getAdId()));
    }

    @GetMapping("/findPage")
    public ResponseEntity<ApiResponse<Page<AdvertisementResponse>>> findAdByPage(
            @RequestParam(defaultValue = "0") Integer pageNumber){

        Page<AdvertisementResponse> advertisements =
                adService.findAdByPage(pageNumber);

        return ResponseEntity.ok(
                ApiResponse.ok("廣告分頁查詢成功", advertisements)
        );
    }

}
