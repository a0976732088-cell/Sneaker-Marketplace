package com.project.sneakermarket.commerce.repository;

import com.project.sneakermarket.commerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // 查某商品主圖（isPrimary = true）為商品列表頁顯示的縮圖（imageUrl）
    List<ProductImage> findByProductProductIdAndIsPrimaryTrue(Long productId);

    // 查某商品「全部」的圖片
    List<ProductImage> findByProductProductId(Long productId);

    //刪除某商品所有圖片
    void deleteByProductProductId(Long productId);
}
