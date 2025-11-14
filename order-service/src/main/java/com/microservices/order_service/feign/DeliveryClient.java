package com.microservices.order_service.feign;

import com.microservices.order_service.model.dto.DeliveryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("DELIVERY-SERVICE")
public interface DeliveryClient {

    @GetMapping("/api/v1/delivery/{deliveryId}")
    DeliveryDto getDeliveryById(@PathVariable("deliveryId") Long deliveryId);
}
