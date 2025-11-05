package com.microservices.order_service.model.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDto {

    private Long productId;
    private String productName;
    private String productDescription;
    private BigDecimal productPrice;
}