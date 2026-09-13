package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.notification.dto.NotificationLogDTO;
import com.project.sneakermarket.notification.entity.NotificationLog;
import com.project.sneakermarket.notification.repository.NotificationLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationLogService {

    private final NotificationLogRepository notificationLogRepo;

    public Page<NotificationLogDTO> searchLogs(
            String title,
            String sendType,
            Integer pageNumber,
            Integer pageSize) {

        Specification<NotificationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (title != null && !title.trim().isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + title.trim() + "%"));
            }
            if (sendType != null && !sendType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("sendType"), sendType.trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int page = (pageNumber != null && pageNumber > 0) ? pageNumber - 1 : 0;
        int size = (pageSize != null && pageSize > 0) ? pageSize : 10;
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");

        // 將 Entity 轉換為 DTO 回傳
        return notificationLogRepo.findAll(spec, pageable).map(log -> {
            NotificationLogDTO dto = new NotificationLogDTO();
            dto.setId(log.getId());
            dto.setNotificationId(log.getNotificationId());

            dto.setSendType(log.getSendType());
            dto.setTitle(log.getTitle());
            dto.setTargetType(log.getTargetType());
            dto.setEventId(log.getEventId());

            dto.setStatus(
                    log.getStatus() != null
                            ? log.getStatus().name()
                            : null
            );

            dto.setTotalRecipients(log.getTotalRecipients());
            dto.setTotalSent(log.getTotalSent());
            dto.setFailedCount(log.getFailedCount());

            dto.setCreatedAt(log.getCreatedAt());
            dto.setStartedAt(log.getStartedAt());
            dto.setCompletedAt(log.getCompletedAt());

            dto.setErrorMessage(log.getErrorMessage());
            return dto;
        });
    }
}
