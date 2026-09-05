package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/**
 * LocalImageStorageService 測試。
 *
 * 使用 JUnit 暫存資料夾測試，
 * 不會真的把測試圖片寫進專案 uploads 目錄。
 */
class LocalImageStorageServiceTests {

    @TempDir
    Path tempDir;

    /**
     * 驗證圖片可以成功寫入本機，
     * 並可透過 delete() 正常刪除。
     */
    @Test
    void storeAndDeleteImage_shouldWorkCorrectly() {

        // Arrange
        LocalImageStorageService imageStorageService =
                new LocalImageStorageService(
                        tempDir.toString()
                );

        MockMultipartFile image = new MockMultipartFile(
                "file",
                "test-shoe.jpg",
                "image/jpeg",
                "fake-image-content".getBytes()
        );

        Long auctionProductId = 100L;

        // Act：儲存圖片
        String imageUrl = imageStorageService.store(
                image,
                auctionProductId
        );

        /*
         * imageUrl 會是：
         * /uploads/auctions/100/{UUID}.jpg
         *
         * Storage 的實際根目錄則是本次測試的 tempDir，
         * 所以從 URL 取出相對路徑來定位檔案。
         */
        String relativePath =
                imageUrl.substring(
                        "/uploads/auctions/".length()
                );

        Path storedFile =
                tempDir.resolve(relativePath);

        // Assert：確認圖片真的存在
        assertTrue(
                Files.exists(storedFile),
                "圖片應該已成功寫入暫存資料夾"
        );

        // Act：刪除圖片
        imageStorageService.delete(imageUrl);

        // Assert：確認圖片真的被刪除
        assertFalse(
                Files.exists(storedFile),
                "圖片應該已成功刪除"
        );
    }
}