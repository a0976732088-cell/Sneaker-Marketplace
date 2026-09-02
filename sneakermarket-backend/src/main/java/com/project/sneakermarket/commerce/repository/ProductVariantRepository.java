package com.project.sneakermarket.commerce.repository;

import com.project.sneakermarket.commerce.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 查出「某個商品」底下所有的規格（尺寸、顏色）
// 用途：算商品價格、之後做商品明細頁的規格列表

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductProductId(Long productId);
}
