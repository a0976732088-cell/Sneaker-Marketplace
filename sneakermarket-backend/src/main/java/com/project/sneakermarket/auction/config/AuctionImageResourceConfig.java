package com.project.sneakermarket.auction.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 競標商品圖片靜態資源設定。
 *
 * 將瀏覽器請求的：
 * /uploads/auctions/**
 *
 * 映射到實際本機目錄：
 * uploads/auctions/
 */
@Configuration
public class AuctionImageResourceConfig implements WebMvcConfigurer {

    private final Path uploadRoot;

    public AuctionImageResourceConfig(
            @Value("${auction.image.upload-dir:uploads/auctions}")
            String uploadDir) {

        this.uploadRoot = Path.of(uploadDir)
                .toAbsolutePath()
                .normalize();
    }

    /**
     * 建立競標圖片的 HTTP URL 與本機檔案目錄映射。
     */
    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry) {

        String resourceLocation =
                uploadRoot.toUri().toString();

        /*
         * Spring ResourceHandler 的目錄位置需要以 / 結尾，
         * 確保後面的子目錄與檔名可以正確接上。
         */
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }

        registry
                .addResourceHandler(
                        "/uploads/auctions/**"
                )
                .addResourceLocations(resourceLocation);
    }
}