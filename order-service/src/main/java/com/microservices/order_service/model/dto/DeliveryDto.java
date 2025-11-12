package com.microservices.order_service.model.dto;

import com.microservices.order_service.model.Status;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeliveryDto {

    private Long deliveryId;
    private String userId;
    private Long orderId;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDate expectedDeliveryDate;
}