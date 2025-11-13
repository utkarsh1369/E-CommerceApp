package com.microservices.delivery_service.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeliveryRequestDto {

    @NotNull(message = "User ID is required")
    private String userId;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private LocalDate expectedDeliveryDate;
}
