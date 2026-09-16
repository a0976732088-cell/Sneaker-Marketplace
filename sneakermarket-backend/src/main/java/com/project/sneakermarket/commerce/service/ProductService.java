package com.project.sneakermarket.commerce.service;

import com.project.sneakermarket.commerce.dto.*;
import com.project.sneakermarket.commerce.entity.*;
import com.project.sneakermarket.commerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    // 取得商品列表，每筆商品只顯示代表價格與一張主圖（給列表頁用）
    // categoryId、keyword、size 都可以不帶（null），代表使用者沒有指定該項篩選條件
    public List<ProductListItemResponse> getAllProducts(Long categoryId, String keyword, String size, Long brandId) {
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
                .filter(p -> brandId == null || p.getBrand().getBrandId().equals(brandId))
                .map(p -> {
                    List<ProductVariant> variants =
                            productVariantRepository.findByProductProductId(p.getProductId());

                    // 同商品所有規格價格相同，直接抓第一筆；沒有規格資料就顯示 null
                    BigDecimal price = variants.isEmpty() ? null : variants.get(0).getPrice();

                    List<ProductImage> images =
                            productImageRepository.findByProductProductIdAndIsPrimaryTrue(p.getProductId());

                    String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

                    int totalStock = variants.stream()
                            .mapToInt(ProductVariant::getStock)
                            .sum();

                    return new ProductListItemResponse(
                            p.getProductId(),
                            p.getProductName(),
                            p.getBrand().getBrandName(),
                            p.getStatus(),
                            price,
                            imageUrl,
                            totalStock);
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
                variantResponses,
                product.getCategory().getCategoryId(),
                product.getBrand().getBrandId());
    }





    // [後台]取得商品列表
    public List<ProductListItemResponse> getAllProductsForAdmin(
            Long categoryId, String keyword, String size, Long brandId, String status, Integer maxStock) {

        List<Product> products = productRepository.findAll();

        List<ProductListItemResponse> result = products.stream()
                // 不寫死「只顯示 ON_SALE」，狀態要不要篩由參數決定
                .filter(p -> categoryId == null || p.getCategory().getCategoryId().equals(categoryId))
                .filter(p -> keyword == null || p.getProductName().toLowerCase().contains(keyword.toLowerCase()))
                .filter(p -> size == null || productVariantRepository.findByProductProductId(p.getProductId())
                        .stream()
                        .anyMatch(v -> v.getSize().equals(size)))
                .filter(p -> brandId == null || p.getBrand().getBrandId().equals(brandId))
                // 有指定狀態就照指定的篩；沒指定就預設排除已刪除（DELETED）的商品
                .filter(p -> status != null ? p.getStatus().equals(status) : !p.getStatus().equals("DELETED"))
                .map(p -> {
                    List<ProductVariant> variants =
                            productVariantRepository.findByProductProductId(p.getProductId());

                    BigDecimal price = variants.isEmpty() ? null : variants.get(0).getPrice();

                    List<ProductImage> images =
                            productImageRepository.findByProductProductIdAndIsPrimaryTrue(p.getProductId());

                    String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

                    int totalStock = variants.stream()
                            .mapToInt(ProductVariant::getStock)
                            .sum();

                    return new ProductListItemResponse(
                            p.getProductId(),
                            p.getProductName(),
                            p.getBrand().getBrandName(),
                            p.getStatus(),
                            price,
                            imageUrl,
                            totalStock);
                })
                .collect(Collectors.toList());

        // 庫存篩選（庫存小於某數字）要等上面 totalStock 算出來才能篩
        if (maxStock != null) {
            result = result.stream()
                    .filter(r -> r.getTotalStock() < maxStock)
                    .collect(Collectors.toList());
        }

        return result;
    }





    // [後台]新增某筆商品
    public ProductDetailResponse createProduct(CreateProductRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("找不到這個商品分類"));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new RuntimeException("找不到這個品牌"));

        Product product = new Product();
        product.setCategory(category);
        product.setBrand( brand);
        product.setProductName(request.getProductName());
        product.setDescription( request.getDescription());
        product.setStatus(request.getStatus() != null ? request.getStatus() : "DRAFT");
        productRepository.save(product);

        for (ProductVariantRequest v : request.getVariants()) {
            ProductVariant variant = new ProductVariant();
            variant.setSize(v.getSize());
            variant.setColor(v.getColor());
            variant.setPrice(v.getPrice());
            variant.setStock(v.getStock());
            variant.setProduct(product);
            variant.setCondition("NEW");
            productVariantRepository.save(variant);
        }

        if (request.getImages() != null) {
            for (ProductImageRequest img : request.getImages()) {
                ProductImage image = new ProductImage();
                image.setImageUrl(img.getImageUrl());
                image.setSortOrder(img.getSortOrder() != null ? img.getSortOrder() : 0);
                image.setIsPrimary(img.getIsPrimary() != null ? img.getIsPrimary() : false);
                image.setProduct(product);
                productImageRepository.save(image);
            }
        }

        return getProductDetail(product.getProductId());
    }



    // [後台]修改某筆商品（含規格的新增/更新/刪除）
    @Transactional
    public ProductDetailResponse updateProduct(Long productId, UpdateProductRequest request) {

        // 找出既有商品
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("找不到這個商品"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("找不到這個商品分類"));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new RuntimeException("找不到這個品牌"));

        // 直接覆蓋既有 product 的欄位
        product.setCategory(category);
        product.setBrand(brand);
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setStatus(request.getStatus() != null ? request.getStatus() : product.getStatus());
        productRepository.save(product);

        // 記錄這次處理完後，應該保留的規格ID
        List<Long> keepIds = new ArrayList<>();

        // 規格逐筆比對新舊
        for (UpdateVariantRequest v : request.getVariants()) {
            if (v.getVariantId() != null) {
                // 有帶 variantId → 是既有規格 → 查出來、更新欄位（維持原 variantId 不變）
                ProductVariant variant = productVariantRepository.findById(v.getVariantId())
                        .orElseThrow(() -> new RuntimeException("找不到這筆規格"));
                variant.setSize(v.getSize());
                variant.setColor(v.getColor());
                variant.setPrice(v.getPrice());
                variant.setStock(v.getStock());
                productVariantRepository.save(variant);
                keepIds.add(variant.getVariantId());
            } else {
                // 沒帶 variantId → 是管理員新加的規格 → 建立全新的一筆
                ProductVariant variant = new ProductVariant();
                variant.setSize(v.getSize());
                variant.setColor(v.getColor());
                variant.setPrice(v.getPrice());
                variant.setStock(v.getStock());
                variant.setProduct(product);
                variant.setCondition("NEW");
                productVariantRepository.save(variant);
                keepIds.add(variant.getVariantId());
            }
        }

        // 刪除檢查：資料庫裡有、但不在 keepIds 裡的規格才刪除
        List<ProductVariant> existingVariants = productVariantRepository.findByProductProductId(productId);
        for (ProductVariant existing : existingVariants) {
            if (!keepIds.contains(existing.getVariantId())) {
                productVariantRepository.delete(existing);
            }
        }

        // 圖片：不用比對新舊，直接整批刪除舊的，再存一份新的
        productImageRepository.deleteByProductProductId(productId);

        if (request.getImages() != null) {
            for (ProductImageRequest img : request.getImages()) {
                ProductImage image = new ProductImage();
                image.setImageUrl(img.getImageUrl());
                image.setSortOrder(img.getSortOrder() != null ? img.getSortOrder() : 0);
                image.setIsPrimary(img.getIsPrimary() != null ? img.getIsPrimary() : false);
                image.setProduct(product);
                productImageRepository.save(image);
            }
        }

        return getProductDetail(product.getProductId());
    }



    // [後台]刪除某筆商品（假刪除：只把狀態改成 DELETED，圖片與規格都不動）
    @Transactional
    public void deleteProduct(Long productId){

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("找不到這個商品"));

        product.setStatus("DELETED");
        productRepository.save(product);

    }





}