package com.microservices.delivery_service.exception;

public class DeliveryNotFoundException extends RuntimeException {

    public DeliveryNotFoundException(String message) {
        super(message);
    }

    public DeliveryNotFoundException(Long deliveryId) {
        super("Delivery not found with ID: " + deliveryId);
    }
}
