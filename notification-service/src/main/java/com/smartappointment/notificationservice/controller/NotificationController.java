package com.smartappointment.notificationservice.controller;

import com.smartappointment.notificationservice.dto.NotificationEmailDto;
import com.smartappointment.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/email")
    public ResponseEntity<Void> envoyerEmail(@Valid @RequestBody NotificationEmailDto request) {
        notificationService.envoyerEmail(request);
        return ResponseEntity.ok().build();
    }
}
