package com.project.sneakermarket.advertisement.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import com.project.sneakermarket.advertisement.entity.AdvertisementPlacement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.project.sneakermarket.common.exception.BusinessException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

@Service
public class LocalAdvertisementImageStorageService implements AdvertisementImageStorageService {

    private record ImageRule(
            String label,
            int minimumWidth,
            int minimumHeight,
            double ratio,
            String ratioLabel
    ) {
    }

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp");

    private static final Map<AdvertisementPlacement, ImageRule> IMAGE_RULES =
            Map.of(
                    AdvertisementPlacement.BANNER,
                    new ImageRule(
                            "首頁橫幅",
                            1200,
                            360,
                            10.0 / 3.0,
                            "10:3"
                    ),

                    AdvertisementPlacement.POPUP,
                    new ImageRule(
                            "首頁彈窗",
                            600,
                            750,
                            4.0 / 5.0,
                            "4:5"
                    ),

                    AdvertisementPlacement.SMALL_BLOCK,
                    new ImageRule(
                            "商品頁側欄",
                            300,
                            300,
                            1.0,
                            "1:1"
                    )
            );

    private static final String URL_PREFIX = "/uploads/advertisements/";

    private final Path uploadRoot;

    public LocalAdvertisementImageStorageService(
            @Value("${advertisement.image.upload-dir:uploads/advertisements}")
            String uploadDir) {

        this.uploadRoot = Path.of(uploadDir)
                .toAbsolutePath()
                .normalize();
    }

    private void validateImageDimensions(
            MultipartFile file,
            AdvertisementPlacement adPlacement
    ) {
        ImageRule rule = IMAGE_RULES.get(adPlacement);

        if (rule == null) {
            throw new BusinessException("廣告版位不可為空或不合法");
        }

        try (
                ImageInputStream imageInputStream =
                        ImageIO.createImageInputStream(file.getInputStream())
        ) {
            if (imageInputStream == null) {
                throw new BusinessException("無法讀取圖片內容");
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(imageInputStream);

            if (!readers.hasNext()) {
                throw new BusinessException(
                        "檔案不是有效的 JPG、PNG 或 WEBP 圖片"
                );
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInputStream, true, true);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (
                        width < rule.minimumWidth()
                                || height < rule.minimumHeight()
                ) {
                    throw new BusinessException(
                            rule.label()
                                    + "圖片至少需要 "
                                    + rule.minimumWidth()
                                    + " × "
                                    + rule.minimumHeight()
                                    + " px，目前圖片為 "
                                    + width
                                    + " × "
                                    + height
                                    + " px"
                    );
                }

                double actualRatio = (double) width / height;

                double ratioDifference =
                        Math.abs(actualRatio - rule.ratio())
                                / rule.ratio();

                if (ratioDifference > 0.02) {
                    throw new BusinessException(
                            rule.label()
                                    + "圖片比例需要接近 "
                                    + rule.ratioLabel()
                                    + "，目前圖片為 "
                                    + width
                                    + " × "
                                    + height
                                    + " px"
                    );
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new BusinessException("圖片內容讀取失敗");
        }
    }

    @Override
    public String store(MultipartFile file, AdvertisementPlacement adPlacement) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上傳圖片不可為空");
        }

        String extension = getExtension(file.getOriginalFilename());

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(
                    "圖片格式僅支援 JPG、JPEG、PNG、WEBP"
            );
        }

        validateImageDimensions(file, adPlacement);

        String storedFileName =
                UUID.randomUUID() + "." + extension;

        Path targetFile = uploadRoot
                .resolve(storedFileName)
                .normalize();

        if (!targetFile.startsWith(uploadRoot)) {
            throw new BusinessException("圖片儲存路徑不合法");
        }

        try {
            Files.createDirectories(uploadRoot);

            Files.copy(
                    file.getInputStream(),
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException e) {
            throw new IllegalStateException("圖片儲存失敗", e);
        }

        return URL_PREFIX + storedFileName;
    }

    @Override
    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) {
            return;
        }

        String fileName = imageUrl.substring(URL_PREFIX.length());

        Path targetFile = uploadRoot
                .resolve(fileName)
                .normalize();

        if (!targetFile.startsWith(uploadRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(targetFile);

        } catch (IOException e) {
            throw new IllegalStateException("圖片刪除失敗", e);
        }
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("無法判斷圖片格式");
        }

        int dotIndex = originalFilename.lastIndexOf(".");

        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            throw new BusinessException("圖片缺少有效副檔名");
        }

        return originalFilename
                .substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }
}