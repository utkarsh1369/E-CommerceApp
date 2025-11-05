package com.microservices.delivery_service.kafka.event;

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

    private String eventType;
    private String orderId;
    private String deliveryId;
    private String userId;
    private String userEmail;
    private String message;
    private String status;
    private LocalDateTime timestamp;
}
