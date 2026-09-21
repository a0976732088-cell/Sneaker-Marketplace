package com.project.sneakermarket.notification.repository;

import com.project.sneakermarket.notification.entity.ProductReleaseNotificationDispatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReleaseNotificationDispatchRepository
        extends JpaRepository<
        ProductReleaseNotificationDispatch,
        Long
        > {

    boolean existsByProductId(Long productId);
}