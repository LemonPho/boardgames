package com.motomutterers.boardgames.notifications.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.motomutterers.boardgames.notifications.dto.NotificationResponse;
import com.motomutterers.boardgames.notifications.services.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(
        NotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @GetMapping()
    public ResponseEntity<List<NotificationResponse>> getNotifications(
        Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.getNotifications(authentication));
    }
}
