package com.microservices.order_service.service;

import com.microservices.order_service.model.dto.DeliveryDto;
import com.microservices.order_service.model.dto.OrderRequestDto;
import com.microservices.order_service.model.dto.OrderResponseDto;

import java.util.List;

public interface OrderService {

    OrderResponseDto createOrder(OrderRequestDto orderRequestDto);
    OrderResponseDto updateOrder(Long orderId, OrderRequestDto orderRequestDto);
    OrderResponseDto findOrderById(Long orderId);
    List<OrderResponseDto> findAllOrders();
    DeliveryDto getDeliveryByOrderId(Long orderId);
}