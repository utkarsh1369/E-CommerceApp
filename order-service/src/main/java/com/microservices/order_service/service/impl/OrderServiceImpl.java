package com.microservices.order_service.service.impl;

import com.microservices.order_service.exception.DeliveryNotFoundException;
import com.microservices.order_service.exception.OrderNotFoundException;
import com.microservices.order_service.exception.ProductServiceException;
import com.microservices.order_service.feign.DeliveryClient;
import com.microservices.order_service.feign.ProductClient;
import com.microservices.order_service.kafka.event.NotificationEvent;
import com.microservices.order_service.kafka.producer.NotificationEventProducer;
import com.microservices.order_service.mapper.OrderMapper;
import com.microservices.order_service.model.OrderItem;
import com.microservices.order_service.model.Orders;
import com.microservices.order_service.model.Status;
import com.microservices.order_service.model.dto.*;
import com.microservices.order_service.repository.OrderRepository;
import com.microservices.order_service.service.OrderService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final DeliveryClient deliveryClient;
    private final OrderMapper orderMapper;
    private final NotificationEventProducer  notificationEventProducer;

    @Override
    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto orderRequestDto) {
        log.info("Creating order for user: {}", orderRequestDto.getUserId());

        BigDecimal orderAmount = calculateOrderAmount(orderRequestDto.getOrderItems());

        Orders order = Orders.builder()
                .userId(orderRequestDto.getUserId())
                .orderAmount(orderAmount)
                .paymentMode(orderRequestDto.getPaymentMode())
                .isPaid(false)
                .status(Status.PENDING)
                .build();

        for (OrderItemDto itemDto : orderRequestDto.getOrderItems()) {
            OrderItem orderItem = orderMapper.toOrderItem(itemDto, order);
            order.addOrderItem(orderItem);
        }
        Orders savedOrder = orderRepository.save(order);

        log.info("Order created successfully with ID: {}", savedOrder.getOrderId());

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .eventType("ORDER_CREATED")
                .orderId(savedOrder.getOrderId().toString())
                .userId(savedOrder.getUserId())
                .message(String.format("Order created successfully with ID: %d. Total amount: ₹%.2f",
                        savedOrder.getOrderId(), savedOrder.getOrderAmount()))
                .status(savedOrder.getStatus().name())
                .timestamp(LocalDateTime.now())
                .build();

        notificationEventProducer.sendNotification(notificationEvent);
        return orderMapper.toResponseDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrder(Long orderId, OrderRequestDto orderRequestDto) {
        log.info("Updating order with ID: {}", orderId);

        Orders existingOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        BigDecimal newOrderAmount = calculateOrderAmount(orderRequestDto.getOrderItems());
        existingOrder.setOrderAmount(newOrderAmount);
        existingOrder.setPaymentMode(orderRequestDto.getPaymentMode());

        existingOrder.getOrderItems().clear();
        for (OrderItemDto itemDto : orderRequestDto.getOrderItems()) {
            OrderItem orderItem = orderMapper.toOrderItem(itemDto, existingOrder);
            existingOrder.addOrderItem(orderItem);
        }
        Orders updatedOrder = orderRepository.save(existingOrder);

        log.info("Order updated successfully with ID: {}", orderId);


        NotificationEvent notificationEvent = NotificationEvent.builder()
                .eventType("ORDER_UPDATED")
                .orderId(updatedOrder.getOrderId().toString())
                .userId(updatedOrder.getUserId())
                .message(String.format("Order #%d has been updated. New total amount: ₹%.2f",
                        updatedOrder.getOrderId(), updatedOrder.getOrderAmount()))
                .status(updatedOrder.getStatus().name())
                .timestamp(LocalDateTime.now())
                .build();

        notificationEventProducer.sendNotification(notificationEvent);
        return orderMapper.toResponseDto(updatedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto findOrderById(Long orderId) {
        log.info("Fetching order with ID: {}", orderId);

        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return orderMapper.toResponseDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> findAllOrders() {
        log.info("Fetching all orders");

        List<Orders> orders = orderRepository.findAll();
        log.info("Found {} orders", orders.size());

        return orderMapper.toResponseDtoList(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryDto getDeliveryByOrderId(Long orderId) {
        log.info("Fetching delivery for order ID: {}", orderId);

        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        Long deliveryId = order.getDeliveryId();
        if (deliveryId == null) {
            throw new DeliveryNotFoundException("No delivery assigned to this order yet.");
        }
        return deliveryClient.getDeliveryById(deliveryId);
    }

    private BigDecimal calculateOrderAmount(List<OrderItemDto> orderItems) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemDto itemDto : orderItems) {
            try {
                ProductDto product = productClient.getProductById(itemDto.getProductId());
                BigDecimal itemTotal = product.getProductPrice()
                        .multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                totalAmount = totalAmount.add(itemTotal);

                log.debug("Product ID: {}, Price: {}, Quantity: {}, Item Total: {}",
                        itemDto.getProductId(), product.getProductPrice(),
                        itemDto.getQuantity(), itemTotal);
            } catch (FeignException e) {
                log.error("Failed to fetch product {}: ", itemDto.getProductId(), e);
                throw new ProductServiceException("Failed to fetch product details for product ID: " + itemDto.getProductId());
            }
        }

        log.info("Calculated order amount: {}", totalAmount);
        return totalAmount;
    }
}