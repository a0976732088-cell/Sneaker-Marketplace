package com.project.sneakermarket.notification.service;


import com.project.sneakermarket.notification.dto.NotificationDTO;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification createNotice(NotificationDTO dto) {
        Notification notice = new Notification();
        notice.setNotificationType(dto.getNotificationType());
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setTargetType(dto.getTargetType());
        notice.setTargetId(dto.getTargetId());
        notice.setTargetUrl(dto.getTargetUrl());

        return notificationRepository.save(notice);
    }
}
