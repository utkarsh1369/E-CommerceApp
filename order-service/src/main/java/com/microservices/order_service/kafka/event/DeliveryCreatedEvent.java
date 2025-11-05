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
public class DeliveryCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long deliveryId;
    private String status;
    private LocalDateTime createdAt;
}
