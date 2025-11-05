package com.microservices.delivery_service.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeliveryRequestDto {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private LocalDateTime expectedDeliveryDate;
}
