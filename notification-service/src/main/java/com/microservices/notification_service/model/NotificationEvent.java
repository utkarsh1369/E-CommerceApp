package com.microservices.notification_service.model;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class NotificationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType;      // ORDER_CREATED, ORDER_UPDATED, DELIVERY_CREATED, DELIVERY_STATUS_CHANGED
    private String orderId;
    private String deliveryId;
    private String userId;
    private String userEmail;
    private String message;
    private String status;         // For status updates
    private LocalDateTime timestamp;
}

