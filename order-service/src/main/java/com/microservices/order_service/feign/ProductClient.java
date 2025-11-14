package com.microservices.order_service.feign;

import com.microservices.order_service.model.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {

    @GetMapping("/api/v1/products/{productId}")
    ProductDto getProductById(@PathVariable("productId") Long productId);
}
