package com.project.sneakermarket.commerce.service;

import com.project.sneakermarket.commerce.dto.ProductListItemResponse;
import com.project.sneakermarket.commerce.entity.Product;
import com.project.sneakermarket.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    //取得所有商品
    public List<ProductListItemResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(p -> new ProductListItemResponse(
                        p.getProductId(),
                        p.getProductName(),
                        p.getBrand().getBrandName(),
                        p.getStatus()
                ))
                .collect(Collectors.toList());
    }
}
