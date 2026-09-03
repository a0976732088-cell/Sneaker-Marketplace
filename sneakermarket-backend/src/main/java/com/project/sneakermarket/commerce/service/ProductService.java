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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    public ProductService(ProductRepository productRepository,
                          ProductVariantRepository productVariantRepository,
                          ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
    }

    //取得商品列表，每筆商品只顯示代表價格與一張主圖（給列表頁用）
    public List<ProductListItemResponse> getAllProducts(Long categoryId, String keyword, String size) {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .filter(p -> p.getStatus().equals("ON_SALE"))
                .filter(p -> categoryId == null || p.getCategory().getCategoryId().equals(categoryId))
                .filter(p -> keyword == null || p.getProductName().toLowerCase().contains(keyword.toLowerCase()))
                .filter(p -> size == null || productVariantRepository.findByProductProductId(p.getProductId())
                        .stream()
                        .anyMatch(v -> v.getSize().equals(size)))
                .map(p -> {
                    List<ProductVariant> variants =
                            productVariantRepository.findByProductProductId(p.getProductId());

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

    //取得單一商品的完整明細，包含全部規格與全部圖片（給明細頁用）
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("找不到這個商品"));

        List<ProductVariant> variants = productVariantRepository.findByProductProductId(productId);
        List<ProductImage> images = productImageRepository.findByProductProductId(productId);

        List<String> imageUrls = images.stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList());

        List<ProductVariantResponse> variantResponses = variants.stream()
                .map(v -> new ProductVariantResponse(v.getSize(), v.getColor(), v.getPrice(), v.getStock()))
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