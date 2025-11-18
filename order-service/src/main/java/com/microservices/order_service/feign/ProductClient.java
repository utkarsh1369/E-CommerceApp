package com.microservices.order_service.feign;

import com.microservices.order_service.model.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {

    @GetMapping("/api/v1/products/{productId}")
    ProductDto getProductById(@PathVariable("productId") Long productId);

    @PatchMapping("/api/v1/products/reduce-stock/{productId}")
    ResponseEntity<ProductDto> reduceStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity);

    @PatchMapping("api/v1/products/increase-stock/{productId}")
    ResponseEntity<ProductDto> increaseStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity);
}
