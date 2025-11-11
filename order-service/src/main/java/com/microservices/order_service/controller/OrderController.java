package com.microservices.order_service.controller;

import com.microservices.order_service.model.dto.DeliveryDto;
import com.microservices.order_service.model.dto.OrderRequestDto;
import com.microservices.order_service.model.dto.OrderResponseDto;
import com.microservices.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto orderRequestDto) {
        OrderResponseDto createdOrder = orderService.createOrder(orderRequestDto);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> updateOrder(@PathVariable Long orderId, @Valid @RequestBody OrderRequestDto orderRequestDto) {
        OrderResponseDto updatedOrder = orderService.updateOrder(orderId, orderRequestDto);
        return ResponseEntity.ok(updatedOrder);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORDER_ADMIN')")
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> findAllOrders() {
        List<OrderResponseDto> orders = orderService.findAllOrders();
        return ResponseEntity.ok(orders);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORDER_ADMIN') or @orderSecurityService.isOrderOwner(#orderId)")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> findOrderById(@PathVariable Long orderId) {
        OrderResponseDto order = orderService.findOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORDER_ADMIN','DELIVERY_ADMIN')")
    @GetMapping("/{orderId}/delivery")
    public ResponseEntity<DeliveryDto> findDeliveryByOrderId(@PathVariable Long orderId) {
        log.info("REST request to get delivery for order ID: {}", orderId);
        DeliveryDto delivery = orderService.getDeliveryByOrderId(orderId);
        return ResponseEntity.ok(delivery);
    }
}