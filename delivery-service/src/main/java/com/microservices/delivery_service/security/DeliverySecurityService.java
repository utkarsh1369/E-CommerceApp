package com.microservices.delivery_service.security;

import com.microservices.delivery_service.exception.DeliveryNotFoundException;
import com.microservices.delivery_service.model.dto.DeliveryDto;
import com.microservices.delivery_service.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliverySecurityService {

    private final DeliveryService deliveryService;


    public boolean isDeliveryOwner(Long deliveryId) {
        // 1. Get the current user's ID from the Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String currentUserId = authentication.getName();

        try {
            // 2. Get the delivery details
            DeliveryDto delivery = deliveryService.findDeliveryById(deliveryId);

            // 3. Compare the user IDs
            boolean isOwner = currentUserId.equals(delivery.getUserId());
            if (!isOwner) {
                log.warn("Security Check Failed: User '{}' is NOT the owner of delivery '{}'. Owner is '{}'.",
                        currentUserId, deliveryId, delivery.getUserId());
            }
            return isOwner;

        } catch (DeliveryNotFoundException e) {
            // If the delivery doesn't exist, they can't be the owner
            log.warn("Security Check: Delivery not found with ID: {}", deliveryId);
            return false;
        } catch (Exception e) {
            log.error("Error during security check for deliveryId {}: {}", deliveryId, e.getMessage());
            return false;
        }
    }
}