package com.microservices.delivery_service.mapper;

import com.microservices.delivery_service.model.dto.DeliveryDto;
import com.microservices.delivery_service.model.dto.DeliveryRequestDto;
import com.microservices.delivery_service.model.Delivery;
import com.microservices.delivery_service.model.Status;
import com.microservices.delivery_service.model.dto.UpdateDeliveryStatusDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeliveryMapper {

    public DeliveryDto toDto(Delivery delivery) {
        if (delivery == null) {
            return null;
        }

        return DeliveryDto.builder()
                .deliveryId(delivery.getDeliveryId())
                .userId(delivery.getUserId())
                .orderId(delivery.getOrderId())
                .status(delivery.getStatus())
                .createdAt(delivery.getCreatedAt())
                .updatedAt(delivery.getUpdatedAt())
                .expectedDeliveryDate(delivery.getExpectedDeliveryDate())
                .build();
    }

    public Delivery toEntity(DeliveryRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Delivery.builder()
                .userId(dto.getUserId())
                .orderId(dto.getOrderId())
                .status(Status.PENDING)
                .expectedDeliveryDate(dto.getExpectedDeliveryDate())
                .build();
    }

    public List<DeliveryDto> toDtoList(List<Delivery> deliveries) {
        return deliveries.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(DeliveryDto dto, Delivery delivery) {
        if (dto == null || delivery == null) {
            return;
        }

        if (dto.getStatus() != null) {
            delivery.setStatus(dto.getStatus());
        }
        if (dto.getExpectedDeliveryDate() != null) {
            delivery.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        }
    }

    public void updateStatusFromDto(UpdateDeliveryStatusDto dto, Delivery delivery) {
        if (dto == null || delivery == null) {
            return;
        }
        delivery.setStatus(dto.getStatus());
    }
}
