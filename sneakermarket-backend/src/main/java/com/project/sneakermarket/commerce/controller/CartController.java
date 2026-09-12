package com.project.sneakermarket.commerce.controller;

import com.project.sneakermarket.commerce.dto.AddToCartRequest;
import com.project.sneakermarket.commerce.dto.CartItemResponse;
import com.project.sneakermarket.commerce.service.CartService;
import com.project.sneakermarket.common.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 查看購物車
    @GetMapping("/api/cart")
    public List<CartItemResponse> getCart(HttpServletRequest request) {

        Long memberId = requireLoggedInMemberId(request);

        return cartService.getCart(memberId);
    }

    // 加入購物車
    @PostMapping("/api/cart/items")
    public void addToCart(@RequestBody AddToCartRequest request,
                          HttpServletRequest httpRequest) {

        Long memberId = requireLoggedInMemberId(httpRequest);

        cartService.addToCart(
                memberId,
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
            @RequestParam(required = false) Boolean selected,
            HttpServletRequest httpRequest) {

        Long memberId = requireLoggedInMemberId(httpRequest);

        cartService.updateCart(memberId, id, quantity, selected);
    }

    // 刪除品項
    @DeleteMapping("/api/cart/items/{id}")
    public void deleteCartItem(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        Long memberId = requireLoggedInMemberId(httpRequest);

        cartService.deleteCartItem(memberId, id);
    }


    // 共用：取得目前 Session 中的 memberId

    private Long requireLoggedInMemberId(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "尚未登入"
            );
        }

        Long memberId = (Long) session.getAttribute("memberId");

        if (memberId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "尚未登入"
            );
        }

        return memberId;
    }


}