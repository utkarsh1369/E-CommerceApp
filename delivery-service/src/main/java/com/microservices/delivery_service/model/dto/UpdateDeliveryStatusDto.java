package com.microservices.delivery_service.model.dto;

import com.microservices.delivery_service.model.Status;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateDeliveryStatusDto {

    @NotNull(message = "Status is required")
    private Status status;
}