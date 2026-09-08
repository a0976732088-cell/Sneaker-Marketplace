package com.project.sneakermarket.notification.repository;

import com.project.sneakermarket.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.awt.print.Pageable;
import java.util.List;

public interface NotificationRepository  extends
        JpaRepository<Notification, Long>,
        JpaSpecificationExecutor<Notification> {

    // 撈取已發送 (isSent = true) 的通知，並以建立時間降冪排序
    List<Notification> findByIsSentTrueOrderByCreatedAtDesc();
}
