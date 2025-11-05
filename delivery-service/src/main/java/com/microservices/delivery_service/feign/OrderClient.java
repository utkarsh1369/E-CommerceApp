package com.microservices.delivery_service.feign;

import com.microservices.delivery_service.config.FeignConfig;
import com.microservices.delivery_service.model.dto.OrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", configuration = FeignConfig.class)
public interface OrderClient {

    @GetMapping("/orders/{orderId}")
    OrderDto getOrderById(@PathVariable("orderId") Long orderId);
}