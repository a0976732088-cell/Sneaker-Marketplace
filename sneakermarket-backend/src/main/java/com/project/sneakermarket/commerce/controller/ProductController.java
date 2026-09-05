package com.project.sneakermarket.commerce.controller;

import com.project.sneakermarket.commerce.dto.ProductDetailResponse;
import com.project.sneakermarket.commerce.dto.ProductListItemResponse;
import com.project.sneakermarket.commerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    //商品列表（給列表頁用）
    @GetMapping("/api/products")
    public List<ProductListItemResponse> getAllProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String  size) {
        return productService.getAllProducts(categoryId, keyword,size);
    }

    //單一商品明細（給明細頁用）
    @GetMapping("/api/products/{id}")
    public ProductDetailResponse getProductDetail(@PathVariable Long id) {

        return productService.getProductDetail(id);
    }
}