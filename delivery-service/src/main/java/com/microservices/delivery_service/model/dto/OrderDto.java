package com.microservices.delivery_service.model.dto;

import com.microservices.delivery_service.model.Status;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDto {

    private Long orderId;
    private String userId;
    private BigDecimal orderAmount;
    private Status orderStatus;
    private boolean isPaid;
    private Long deliveryId;
    private LocalDateTime createdAt;
}