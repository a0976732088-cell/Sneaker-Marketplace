package com.project.sneakermarket.commerce.service;

import com.project.sneakermarket.commerce.dto.ProductDetailResponse;
import com.project.sneakermarket.commerce.dto.ProductListItemResponse;
import com.project.sneakermarket.commerce.dto.ProductVariantResponse;
import com.project.sneakermarket.commerce.entity.Product;
import com.project.sneakermarket.commerce.entity.ProductImage;
import com.project.sneakermarket.commerce.entity.ProductVariant;
import com.project.sneakermarket.commerce.repository.ProductImageRepository;
import com.project.sneakermarket.commerce.repository.ProductRepository;
import com.project.sneakermarket.commerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    // 取得商品列表，每筆商品只顯示代表價格與一張主圖（給列表頁用）
    // categoryId、keyword、size 都可以不帶（null），代表使用者沒有指定該項篩選條件
    public List<ProductListItemResponse> getAllProducts(Long categoryId, String keyword, String size) {
        List<Product> products = productRepository.findAll();

        return products.stream()
                // 固定規則：不管有沒有篩選條件，永遠只顯示上架中的商品
                .filter(p -> p.getStatus().equals("ON_SALE"))
                .filter(p -> categoryId == null || p.getCategory().getCategoryId().equals(categoryId))
                .filter(p -> keyword == null || p.getProductName().toLowerCase().contains(keyword.toLowerCase()))
                // 尺寸篩選（可選）：跨表查這個商品的規格裡，有沒有符合指定尺寸的
                .filter(p -> size == null || productVariantRepository.findByProductProductId(p.getProductId())
                        .stream()
                        .anyMatch(v -> v.getSize().equals(size)))
                .map(p -> {
                    List<ProductVariant> variants =
                            productVariantRepository.findByProductProductId(p.getProductId());

                    // 同商品所有規格價格相同，直接抓第一筆；沒有規格資料就顯示 null
                    BigDecimal price = variants.isEmpty() ? null : variants.get(0).getPrice();

                    List<ProductImage> images =
                            productImageRepository.findByProductProductIdAndIsPrimaryTrue(p.getProductId());

                    String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

                    return new ProductListItemResponse(
                            p.getProductId(),
                            p.getProductName(),
                            p.getBrand().getBrandName(),
                            p.getStatus(),
                            price,
                            imageUrl);
                })
                .collect(Collectors.toList());
    }

    // 取得單一商品的完整明細，包含全部規格與全部圖片（給明細頁用）
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("找不到這個商品"));

        List<ProductVariant> variants = productVariantRepository.findByProductProductId(productId);
        List<ProductImage> images = productImageRepository.findByProductProductId(productId);

        List<String> imageUrls = images.stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList());

        List<ProductVariantResponse> variantResponses = variants.stream()
                .map(v -> new ProductVariantResponse(v.getSize(), v.getColor(), v.getPrice(), v.getStock(),v.getVariantId()))
                .collect(Collectors.toList());

        return new ProductDetailResponse(
                product.getProductId(),
                product.getProductName(),
                product.getBrand().getBrandName(),
                product.getStatus(),
                product.getDescription(),
                imageUrls,
                variantResponses);
    }

}