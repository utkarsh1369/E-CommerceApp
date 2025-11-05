package com.microservices.order_service.mapper;

import com.microservices.order_service.model.OrderItem;
import com.microservices.order_service.model.Orders;
import com.microservices.order_service.model.dto.OrderItemDto;
import com.microservices.order_service.model.dto.OrderResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponseDto toResponseDto(Orders orders) {
        if (orders == null) {
            return null;
        }

        List<OrderItemDto> items = orders.getOrderItems().stream()
                .map(item -> OrderItemDto.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return OrderResponseDto.builder()
                .orderId(orders.getOrderId())
                .userId(orders.getUserId())
                .orderAmount(orders.getOrderAmount())
                .orderStatus(orders.getStatus())
                .paymentMode(orders.getPaymentMode())
                .isPaid(orders.isPaid())
                .deliveryId(orders.getDeliveryId())
                .createdAt(orders.getCreatedAt())
                .updatedAt(orders.getUpdatedAt())
                .orderItems(items)
                .build();
    }

    public List<OrderResponseDto> toResponseDtoList(List<Orders> ordersList) {
        return ordersList.stream()
                .map(this::toResponseDto)
                .toList();
    }

    public OrderItem toOrderItem(OrderItemDto dto, Orders orders) {
        return OrderItem.builder()
                .productId(dto.getProductId())
                .quantity(dto.getQuantity())
                .orders(orders)
                .build();
    }
}
