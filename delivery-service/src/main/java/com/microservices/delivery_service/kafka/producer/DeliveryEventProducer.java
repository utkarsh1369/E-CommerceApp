package com.microservices.delivery_service.kafka.producer;

import com.microservices.delivery_service.kafka.event.DeliveryCreatedEvent;
import com.microservices.delivery_service.kafka.event.DeliveryStatusChangedEvent;
import com.microservices.delivery_service.kafka.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Component
public class DeliveryEventProducer {

    private static final String TOPIC = "delivery-created";
    private static final String NOTIFICATION_TOPIC = "delivery-events";
    private static final String DELIVERY_STATUS_CHANGED_TOPIC = "delivery-status-changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendDeliveryCreatedEvent(DeliveryCreatedEvent event) {
        log.info("Publishing delivery created event: {}", event);

        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Event published successfully to topic '{}': orderId={}, deliveryId={}",
                            TOPIC, event.getOrderId(), event.getDeliveryId());
                } else {
                    log.error("Failed to publish event to topic '{}': {}", TOPIC, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing delivery created event: {}", e.getMessage(), e);
        }
    }

    public void sendDeliveryStatusChangedEvent(DeliveryStatusChangedEvent event) {
        log.info("Publishing delivery status changed event: {}", event);

        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(DELIVERY_STATUS_CHANGED_TOPIC, event.getDeliveryId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Status change event published successfully to topic '{}': deliveryId={}, status={}",
                            DELIVERY_STATUS_CHANGED_TOPIC, event.getDeliveryId(), event.getNewStatus());
                } else {
                    log.error("Failed to publish status change event to topic '{}': {}",
                            DELIVERY_STATUS_CHANGED_TOPIC, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing delivery status changed event: {}", e.getMessage(), e);
        }
    }

    public void sendNotification(NotificationEvent event) {
        log.info("Publishing notification event to topic '{}': {}", NOTIFICATION_TOPIC, event);

        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(NOTIFICATION_TOPIC, event.getDeliveryId(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Notification event published successfully: {}", event.getEventType());
                } else {
                    log.error("Failed to publish notification event: {}", ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing notification event: {}", e.getMessage(), e);
        }
    }
}
