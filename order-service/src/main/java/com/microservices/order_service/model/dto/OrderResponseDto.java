package com.microservices.order_service.model.dto;

import com.microservices.order_service.model.PaymentMode;
import com.microservices.order_service.model.Status;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponseDto {

    private Long orderId;
    private String userId;
    private BigDecimal orderAmount;
    private Status orderStatus;
    private PaymentMode paymentMode;
    private boolean isPaid;
    private Long deliveryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDto> orderItems;
}