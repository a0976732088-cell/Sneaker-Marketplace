package com.project.sneakermarket.commerce.controller;

import com.project.sneakermarket.commerce.dto.CreateProductRequest;
import com.project.sneakermarket.commerce.dto.ProductDetailResponse;
import com.project.sneakermarket.commerce.dto.ProductListItemResponse;
import com.project.sneakermarket.commerce.dto.UpdateProductRequest;
import com.project.sneakermarket.commerce.service.ProductImageStorageService;
import com.project.sneakermarket.commerce.service.ProductService;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminProductController {

    private final MemberService memberService;
    private final ProductService productService;
    private final ProductImageStorageService productImageStorageService;

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

    // 共用：確認目前登入者是 ROLE_ADMIN
    private Long requireAdmin(HttpServletRequest request) {
        Long memberId = requireLoggedInMemberId(request);
        if (!memberService.isAdmin(memberId)) {
            throw new ForbiddenException("需要管理員權限");
        }
        return memberId;
    }


    // 管理員查看全部商品（不篩狀態）
    @GetMapping("/api/admin/products")
    public List<ProductListItemResponse> getAllProductsForAdmin(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer maxStock,
            HttpServletRequest httpRequest) {

        requireAdmin(httpRequest);
        return productService.getAllProductsForAdmin(categoryId, keyword, size, brandId, status, maxStock);
    }


    // 管理員新增商品
    @PostMapping("/api/admin/products")
    public ProductDetailResponse createProduct(@RequestBody CreateProductRequest request,
                                               HttpServletRequest httpRequest){

        requireAdmin(httpRequest);

        return productService.createProduct(request);
    }


    // 管理員修改商品
    @PutMapping("/api/admin/products/{id}")
    public ProductDetailResponse updateProduct(@PathVariable Long id,
                                               @RequestBody UpdateProductRequest request,
                                               HttpServletRequest httpRequest){

        requireAdmin(httpRequest);
        return productService.updateProduct(id,request);
    }
    


    // 管理員上傳商品圖片
    @PostMapping("/api/admin/products/upload-image")
    public String uploadProductImage(@RequestParam("file") MultipartFile file,
                                     HttpServletRequest httpRequest){
        requireAdmin(httpRequest);
        return productImageStorageService.store(file);
    }
}
