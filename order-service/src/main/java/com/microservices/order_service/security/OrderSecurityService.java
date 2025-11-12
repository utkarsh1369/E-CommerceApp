package com.microservices.order_service.security;

import com.microservices.order_service.exception.OrderNotFoundException;
import com.microservices.order_service.model.dto.OrderResponseDto;
import com.microservices.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSecurityService {

    private final OrderService orderService;

    public boolean isOrderOwner(Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String currentUserId = authentication.getName();

        try {
            OrderResponseDto order = orderService.findOrderById(orderId);

            boolean isOwner = currentUserId.equals(order.getUserId());
            if (!isOwner) {
                log.warn("Security Check Failed: User '{}' is NOT the owner of order '{}'. Owner is '{}'.",
                        currentUserId, orderId, order.getUserId());
            }
            return isOwner;
        } catch (OrderNotFoundException e) {
            log.warn("Security Check: Order not found with ID: {}", orderId);
            return false;
        } catch (Exception e) {
            log.error("Error during security check for orderId {}: {}", orderId, e.getMessage());
            return false;
        }
    }
}