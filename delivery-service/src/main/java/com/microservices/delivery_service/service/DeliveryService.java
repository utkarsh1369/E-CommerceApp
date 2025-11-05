package com.microservices.delivery_service.service;

import com.microservices.delivery_service.model.dto.DeliveryDto;
import com.microservices.delivery_service.model.dto.DeliveryRequestDto;
import com.microservices.delivery_service.model.dto.OrderDto;
import com.microservices.delivery_service.model.dto.UpdateDeliveryStatusDto;

import java.util.List;

public interface DeliveryService {

    DeliveryDto createDelivery(DeliveryRequestDto deliveryRequestDto);

    DeliveryDto updateDeliveryStatus(Long deliveryId, UpdateDeliveryStatusDto statusDto);

    DeliveryDto findDeliveryById(Long deliveryId);

    List<DeliveryDto> findAllDeliveries();

    OrderDto findOrderByDeliveryId(Long deliveryId);
}