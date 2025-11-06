package com.microservices.order_service.exception;

public class DeliveryNotFoundException extends RuntimeException {
    public DeliveryNotFoundException(String message) {
        super(message);
    }
    public DeliveryNotFoundException() {
    }
}
