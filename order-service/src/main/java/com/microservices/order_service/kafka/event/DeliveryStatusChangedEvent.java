package com.microservices.order_service.kafka.event;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DeliveryStatusChangedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long deliveryId;
    private Long orderId;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime changedAt;
}
