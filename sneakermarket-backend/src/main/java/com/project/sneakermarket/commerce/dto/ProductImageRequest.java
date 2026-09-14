package com.project.sneakermarket.commerce.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
//後台「接收」商品[圖片資料]
public class ProductImageRequest {

    private String imageUrl;
    private Integer sortOrder;
    private Boolean isPrimary;
}
