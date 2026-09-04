package com.project.sneakermarket.notification.controller;

import com.project.sneakermarket.notification.dto.NotificationDTO;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/api/notification/createNotice")
    public ResponseEntity<Notification>  createNotice(@RequestBody NotificationDTO request){
        Notification created = notificationService.createNotice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
