package com.project.sneakermarket.commerce.controller;

import com.project.sneakermarket.commerce.dto.AddToCartRequest;
import com.project.sneakermarket.commerce.dto.CartItemResponse;
import com.project.sneakermarket.commerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 查看購物車
    @GetMapping("/api/cart")
    public List<CartItemResponse> getCart(@RequestParam Long memberId) {
        return cartService.getCart(memberId);
    }

    // 加入購物車
    @PostMapping("/api/cart/items")
    public void addToCart(@RequestBody AddToCartRequest request) {
        cartService.addToCart(
                request.getMemberId(),
                request.getVariantId(),
                request.getQuantity()
        );
    }

    // 改數量
    @PutMapping("/api/cart/items/{id}")
    public void updateCart(@PathVariable Long id, @RequestParam Integer quantity) {
        cartService.updateCart(id, quantity);
    }

    // 刪除品項
    @DeleteMapping("/api/cart/items/{id}")
    public void deleteCartItem(@PathVariable Long id) {
        cartService.deleteCartItem(id);
    }
}