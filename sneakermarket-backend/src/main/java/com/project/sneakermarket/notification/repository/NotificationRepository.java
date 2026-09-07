package com.project.sneakermarket.notification.repository;

import com.project.sneakermarket.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.awt.print.Pageable;

public interface NotificationRepository  extends
        JpaRepository<Notification, Long>,
        JpaSpecificationExecutor<Notification> {
}
