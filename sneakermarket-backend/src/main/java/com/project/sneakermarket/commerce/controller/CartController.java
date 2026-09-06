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
    // quantity、selected 都是可選參數，各自代表「改數量」或「勾選/取消勾選」其中一種操作
    @PutMapping("/api/cart/items/{id}")
    public void updateCart(
            @PathVariable Long id,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) Boolean selected) {
        cartService.updateCart(id, quantity, selected);
    }

    // 刪除品項
    @DeleteMapping("/api/cart/items/{id}")
    public void deleteCartItem(@PathVariable Long id) {
        cartService.deleteCartItem(id);
    }
}