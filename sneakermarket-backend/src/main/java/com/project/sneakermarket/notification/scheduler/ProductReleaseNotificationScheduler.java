package com.project.sneakermarket.notification.scheduler;

import com.project.sneakermarket.notification.service.ProductReleaseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "notification.product-release.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ProductReleaseNotificationScheduler {

    private final ProductReleaseNotificationService
            productReleaseNotificationService;

    /*
     * 啟動後先等待 30 秒，再每 60 秒檢查一次。
     */
    @Scheduled(
            initialDelayString =
                    "${notification.product-release.initial-delay-ms:30000}",
            fixedDelayString =
                    "${notification.product-release.poll-ms:60000}"
    )
    public void scanReleasedProducts() {
        List<Long> productIds =
                productReleaseNotificationService
                        .findPendingProductIds();

        for (Long productId : productIds) {
            try {
                productReleaseNotificationService
                        .processProduct(productId);

            } catch (RuntimeException exception) {
                /*
                 * 單一商品失敗不阻止其他商品。
                 * 不輸出 Email、會員等敏感資料。
                 */
                log.error(
                        "新品通知處理失敗，productId={}",
                        productId,
                        exception
                );
            }
        }
    }
}