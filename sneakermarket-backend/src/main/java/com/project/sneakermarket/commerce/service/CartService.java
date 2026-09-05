package com.project.sneakermarket.commerce.service;

import com.project.sneakermarket.commerce.dto.CartItemResponse;
import com.project.sneakermarket.commerce.entity.Cart;
import com.project.sneakermarket.commerce.entity.CartItem;
import com.project.sneakermarket.commerce.entity.ProductImage;
import com.project.sneakermarket.commerce.entity.ProductVariant;
import com.project.sneakermarket.commerce.repository.CartItemRepository;
import com.project.sneakermarket.commerce.repository.CartRepository;
import com.project.sneakermarket.commerce.repository.ProductImageRepository;
import com.project.sneakermarket.commerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;

    // 查看購物車
    public List<CartItemResponse> getCart(Long memberId) {

        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("找不到這個購物車"));

        List<CartItem> cartItems = cartItemRepository.findByCartCartId(cart.getCartId());

        return cartItems.stream()
                .map(item -> {
                    Long productId = item.getVariant().getProduct().getProductId();

                    List<ProductImage> images =
                            productImageRepository.findByProductProductIdAndIsPrimaryTrue(productId);
                    String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

                    return new CartItemResponse(
                            item.getCartItemId(),
                            item.getVariant().getProduct().getProductName(),
                            imageUrl,
                            item.getVariant().getSize(),
                            item.getVariant().getColor(),
                            item.getQuantity(),
                            item.getSelected(),
                            item.getVariant().getPrice()
                    );
                })
                .collect(Collectors.toList());
    }

    // 加入購物車：已有該規格就合併數量，沒有就新增一筆
    public void addToCart(Long memberId, Long variantId, Integer quantity) {

        // 這裡跟 getCart 不同：查不到購物車不算錯誤，而是第一次使用購物車，直接建一台新的
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setMemberId(memberId);
                    return cartRepository.save(newCart);
                });

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("找不到這個規格"));

        // 檢查是否已有這個規格，決定合併還是新增
        Optional<CartItem> existingItem =
                cartItemRepository.findByCartCartIdAndVariantVariantId(cart.getCartId(), variantId);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;

            // 庫存規則：要比合併後的總數，不是只看這次新加的數量
            if (newQuantity > variant.getStock()) {
                throw new RuntimeException("庫存不足");
            }

            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            if (quantity > variant.getStock()) {
                throw new RuntimeException("庫存不足");
            }

            CartItem newCartItem = new CartItem();
            newCartItem.setCart(cart);
            newCartItem.setVariant(variant);
            newCartItem.setQuantity(quantity);
            newCartItem.setSelected(true); // 預設勾選
            cartItemRepository.save(newCartItem);
        }
    }

    // 改數量
    public void updateCart(Long cartItemId, Integer quantity) {

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("找不到這個購物車品項"));

        if (quantity > item.getVariant().getStock()) {
            throw new RuntimeException("庫存不足");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    // 刪除品項
    public void deleteCartItem(Long cartItemId) {

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("找不到這個購物車品項"));

        cartItemRepository.delete(item);
    }
}