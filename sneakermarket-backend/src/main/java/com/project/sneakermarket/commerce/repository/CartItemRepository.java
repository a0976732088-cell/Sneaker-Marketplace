package com.project.sneakermarket.commerce.repository;

import com.project.sneakermarket.commerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // 查看購物車頁面用：列出這台購物車裡全部的品項
    List<CartItem> findByCartCartId(Long cartId);

    // 加入購物車前先檢查：這個規格這台購物車裡是否已經有了
    // 有的話要「合併數量」而不是新增一筆，沒有才新增新的一筆
    Optional<CartItem> findByCartCartIdAndVariantVariantId(Long cartId, Long variantId);


}
