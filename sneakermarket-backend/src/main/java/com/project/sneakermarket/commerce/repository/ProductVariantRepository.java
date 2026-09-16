package com.project.sneakermarket.commerce.repository;

import com.project.sneakermarket.commerce.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // 查出「某個商品」底下所有的規格（尺寸、顏色）
    // 用途：算商品價格、之後做商品明細頁的規格列表
    List<ProductVariant> findByProductProductId(Long productId);

    //刪除某商品所有的規格
    void deleteByProductProductId(Long productId);
}
