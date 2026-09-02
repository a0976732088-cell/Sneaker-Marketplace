package com.project.sneakermarket.commerce.repository;

import com.project.sneakermarket.commerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 查出「某個商品」被標記為主圖（isPrimary = true）的圖片
// 用途：商品列表頁顯示縮圖（thumbnailUrl）

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductProductIdAndIsPrimaryTrue(Long productId);
}
