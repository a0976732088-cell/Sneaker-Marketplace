package com.project.sneakermarket.commerce.controller;

import com.project.sneakermarket.commerce.dto.ProductDetailResponse;
import com.project.sneakermarket.commerce.dto.ProductListItemResponse;
import com.project.sneakermarket.commerce.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/products")
    public List<ProductListItemResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/api/products/{id}")
    public ProductDetailResponse getProductDetail(@PathVariable Long id) {
        return productService.getProductDetail(id);
    }
}