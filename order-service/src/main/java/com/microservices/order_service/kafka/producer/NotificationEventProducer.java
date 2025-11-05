package com.microservices.order_service.kafka.producer;

import com.microservices.order_service.kafka.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationEventProducer {

    private static final String TOPIC = "order-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendNotification(NotificationEvent event) {
        log.info("Publishing notification event to topic '{}': {}", TOPIC, event);

        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(TOPIC, event.getOrderId(), event);

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
