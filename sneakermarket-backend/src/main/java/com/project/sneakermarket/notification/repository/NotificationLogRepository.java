package com.project.sneakermarket.notification.repository;

import com.project.sneakermarket.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface
NotificationLogRepository extends JpaRepository<NotificationLog, Long>, JpaSpecificationExecutor<NotificationLog>
{
}
