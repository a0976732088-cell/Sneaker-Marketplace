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
import java.util.Optional;
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

    public List<ProductListItemResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();

        return products.stream()
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