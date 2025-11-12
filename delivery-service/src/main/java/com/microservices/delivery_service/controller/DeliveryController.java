package com.microservices.delivery_service.controller;

import com.microservices.delivery_service.model.dto.DeliveryDto;
import com.microservices.delivery_service.model.dto.DeliveryRequestDto;
import com.microservices.delivery_service.model.dto.OrderDto;
import com.microservices.delivery_service.model.dto.UpdateDeliveryStatusDto;
import com.microservices.delivery_service.service.DeliveryService;
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
@RequestMapping("/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DELIVERY_ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<DeliveryDto> createDelivery(@Valid @RequestBody DeliveryRequestDto deliveryRequestDto) {
        DeliveryDto createdDelivery = deliveryService.createDelivery(deliveryRequestDto);
        return new ResponseEntity<>(createdDelivery, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DELIVERY_ADMIN')")
    @PatchMapping("/{deliveryId}/update-status")
    public ResponseEntity<DeliveryDto> updateDeliveryStatus(@PathVariable Long deliveryId, @Valid @RequestBody UpdateDeliveryStatusDto statusDto) {
        DeliveryDto updatedDelivery = deliveryService.updateDeliveryStatus(deliveryId, statusDto);
        return ResponseEntity.ok(updatedDelivery);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DELIVERY_ADMIN')")
    @GetMapping
    public ResponseEntity<List<DeliveryDto>> getAllDeliveries() {
        List<DeliveryDto> deliveries = deliveryService.findAllDeliveries();
        return ResponseEntity.ok(deliveries);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DELIVERY_ADMIN') or @DeliverySecurityService.isDeliveryOwner(#deliveryId)")
    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryDto> getDeliveryById(@PathVariable Long deliveryId) {
        DeliveryDto delivery = deliveryService.findDeliveryById(deliveryId);
        return ResponseEntity.ok(delivery);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DELIVERY_ADMIN','ORDER_ADMIN')")
    @GetMapping("/{deliveryId}/order")
    public ResponseEntity<OrderDto> getOrderByDeliveryId(@PathVariable Long deliveryId) {
        OrderDto order = deliveryService.findOrderByDeliveryId(deliveryId);
        return ResponseEntity.ok(order);
    }
}