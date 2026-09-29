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
import com.project.sneakermarket.common.exception.ForbiddenException;
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

        // 新會員還沒加過商品時不會有購物車，這不是錯誤，視為空購物車
        Cart cart = cartRepository.findByMemberId(memberId).orElse(null);
        if (cart == null) {
            return List.of();
        }

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
                            item.getVariant().getPrice(),
                            item.getVariant().getStock()
                    );
                })
                .collect(Collectors.toList());
    }

    // 加入購物車：已有該規格就合併數量，沒有就新增一筆
    public void addToCart(Long memberId, Long variantId, Integer quantity) {

        // 查不到購物車代表第一次使用，直接建一台新的（getCart 只回空清單、不建立，因為查看不該寫入資料）
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setMemberId(memberId);
                    return cartRepository.save(newCart);
                });

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("找不到這個規格"));

        // 只有販售中的商品能加入購物車（防止有人直接用網址或 API 把下架/草稿商品加進來）
        if (!"ON_SALE".equals(variant.getProduct().getStatus())) {
            throw new RuntimeException("此商品目前未販售");
        }

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
    public void updateCart(Long memberId,Long cartItemId, Integer quantity, Boolean selected) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("找不到購物車品項"));

        // 所有權驗證：這筆品項所屬的購物車，必須是目前登入者自己的
        if (!item.getCart().getMemberId().equals(memberId)) {
            throw new ForbiddenException("無權操作他人的購物車品項");
        }

        if (quantity != null) {
            // 合併後的數量不能超過庫存，才不會允許超賣
            if (quantity > item.getVariant().getStock()) {
                throw new RuntimeException("庫存不足");
            }
            item.setQuantity(quantity);
        }

        if (selected != null) {
            item.setSelected(selected);
        }

        cartItemRepository.save(item);
    }

    // 刪除品項
    public void deleteCartItem(Long memberId, Long cartItemId) {

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("找不到這個購物車品項"));

        // 所有權驗證：這筆品項所屬的購物車，必須是目前登入者自己的
        if (!item.getCart().getMemberId().equals(memberId)) {
            throw new ForbiddenException("無權操作他人的購物車品項");
        }

        cartItemRepository.delete(item);
    }
}