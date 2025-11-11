package com.microservices.delivery_service.service.impl;

import com.microservices.delivery_service.exception.UserServiceException;
import com.microservices.delivery_service.feign.UserClient;
import com.microservices.delivery_service.kafka.event.DeliveryCreatedEvent;
import com.microservices.delivery_service.kafka.event.DeliveryStatusChangedEvent;
import com.microservices.delivery_service.kafka.event.NotificationEvent;
import com.microservices.delivery_service.kafka.producer.DeliveryEventProducer;
import com.microservices.delivery_service.model.Status;
import com.microservices.delivery_service.model.dto.*;
import com.microservices.delivery_service.exception.DeliveryNotFoundException;
import com.microservices.delivery_service.exception.OrderServiceException;
import com.microservices.delivery_service.feign.OrderClient;
import com.microservices.delivery_service.mapper.DeliveryMapper;
import com.microservices.delivery_service.model.Delivery;
import com.microservices.delivery_service.repository.DeliveryRepository;
import com.microservices.delivery_service.service.DeliveryService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderClient orderClient;
    private final DeliveryMapper deliveryMapper;
    private final DeliveryEventProducer deliveryEventProducer;
    private final UserClient userClient;

    @Override
    @Transactional
    public DeliveryDto createDelivery(DeliveryRequestDto deliveryRequestDto) {
        try {
            OrderDto order = orderClient.getOrderById(deliveryRequestDto.getOrderId());
        } catch (FeignException e) {
            throw new OrderServiceException("Order not found or Order Service unavailable");
        }
        // Create delivery
        Delivery delivery = deliveryMapper.toEntity(deliveryRequestDto);
        Delivery savedDelivery = deliveryRepository.save(delivery);

        DeliveryCreatedEvent event = DeliveryCreatedEvent.builder()
                .orderId(savedDelivery.getOrderId())
                .deliveryId(savedDelivery.getDeliveryId())
                .status(savedDelivery.getStatus().name())
                .createdAt(savedDelivery.getCreatedAt())
                .build();

        deliveryEventProducer.sendDeliveryCreatedEvent(event);

        UserDto user = null;
        try {
            user = userClient.getUserById(savedDelivery.getUserId().toString());
        } catch (FeignException e) {
           throw new UserServiceException("User not found or UserService unavailable");
        }

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .eventType("DELIVERY_CREATED")
                .orderId(savedDelivery.getOrderId().toString())
                .deliveryId(savedDelivery.getDeliveryId().toString())
                .userId(savedDelivery.getUserId().toString())
                .userEmail(user !=null ? user.getEmail() : null)
                .message(String.format("Delivery for your order #%d. is scheduled with Delivery ID: %d. Expected delivery: %s", savedDelivery.getOrderId(), savedDelivery.getDeliveryId(),
                        savedDelivery.getExpectedDeliveryDate()))
                .status(savedDelivery.getStatus().name())
                .timestamp(LocalDateTime.now())
                .build();

        deliveryEventProducer.sendNotification(notificationEvent);
        return deliveryMapper.toDto(savedDelivery);
    }

    private String getUserEmail(String userId) {
        try{
            UserDto user = userClient.getUserById(userId);
            if(user != null && user.getEmail() != null){
                return user.getEmail();
            }else{
                log.warn("User with id {} not found", userId);
                return "unkonwn@example.com";
            }
        }catch (Exception e){
            return "unkonwn@example.com";
        }
    }

    @Override
    @Transactional
    public DeliveryDto updateDeliveryStatus(Long deliveryId, UpdateDeliveryStatusDto statusDto) {
        log.info("Updating delivery status for ID: {} to {}", deliveryId, statusDto.getStatus());

        Delivery existingDelivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));
        Status oldStatus = existingDelivery.getStatus();
        validateStatusTransition(oldStatus, statusDto.getStatus());
        deliveryMapper.updateStatusFromDto(statusDto, existingDelivery);
        Delivery updatedDelivery = deliveryRepository.save(existingDelivery);

        log.info("Delivery status updated successfully: {} → {}", oldStatus, statusDto.getStatus());

        DeliveryStatusChangedEvent event = DeliveryStatusChangedEvent.builder()
                .deliveryId(updatedDelivery.getDeliveryId())
                .orderId(updatedDelivery.getOrderId())
                .oldStatus(oldStatus.name())
                .newStatus(statusDto.getStatus().name())
                .changedAt(LocalDateTime.now())
                .build();

        deliveryEventProducer.sendDeliveryStatusChangedEvent(event);

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .eventType("DELIVERY_STATUS_CHANGED")
                .orderId(updatedDelivery.getOrderId().toString())
                .deliveryId(updatedDelivery.getDeliveryId().toString())
                .userId(updatedDelivery.getUserId().toString())
                .userEmail(getUserEmail(updatedDelivery.getUserId().toString()))
                .message(String.format("Delivery status updated to: %s for Order #%d",
                        statusDto.getStatus().name(), updatedDelivery.getOrderId()))
                .status(statusDto.getStatus().name())
                .timestamp(LocalDateTime.now())
                .build();

        deliveryEventProducer.sendNotification(notificationEvent);
        return deliveryMapper.toDto(updatedDelivery);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryDto findDeliveryById(Long deliveryId) {

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        return deliveryMapper.toDto(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDto> findAllDeliveries() {

        List<Delivery> deliveries = deliveryRepository.findAll();

        return deliveryMapper.toDtoList(deliveries);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto findOrderByDeliveryId(Long deliveryId) {

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

        Long orderId = delivery.getOrderId();
        if (orderId == null) {
            throw new RuntimeException("No order assigned to this delivery yet.");
        }

        try {
            return orderClient.getOrderById(orderId);
        } catch (FeignException e) {
            throw new OrderServiceException("Failed to fetch order details", e);
        }
    }

    private void validateStatusTransition(Status currentStatus, Status newStatus) {
        switch (currentStatus) {
            case PENDING:
                if (newStatus != Status.SHIPPED && newStatus != Status.CANCELLED) {
                    throw new IllegalStateException(
                            "Cannot transition from PENDING to " + newStatus + ". Only SHIPPED or CANCELLED allowed.");
                }
                break;
            case SHIPPED:
                if (newStatus != Status.DELIVERED && newStatus != Status.CANCELLED) {
                    throw new IllegalStateException(
                            "Cannot transition from SHIPPED to " + newStatus + ". Only DELIVERED or CANCELLED allowed.");
                }
                break;
            case DELIVERED:
                throw new IllegalStateException("Cannot change status of DELIVERED delivery. Delivery is complete.");
            case CANCELLED:
                throw new IllegalStateException("Cannot change status of CANCELLED delivery.");
            default:
                throw new IllegalStateException("Unknown status: " + currentStatus);
        }
    }
}