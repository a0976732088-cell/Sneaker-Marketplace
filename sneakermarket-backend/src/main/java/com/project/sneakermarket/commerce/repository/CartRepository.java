package com.project.sneakermarket.commerce.repository;

import com.project.sneakermarket.commerce.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    // 找這是誰的購物車
    Optional<Cart> findByMemberId(Long memberId);
}
