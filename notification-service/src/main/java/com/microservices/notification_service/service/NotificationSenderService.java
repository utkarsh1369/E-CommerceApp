package com.microservices.notification_service.service;

import com.microservices.notification_service.model.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationSenderService {

    public void sendNotification(NotificationEvent event) {
        log.info("🔔 {} | To: {} | {}",
                event.getEventType(),
                event.getUserEmail(),
                event.getMessage());
    }
}
