package com.microservices.notification_service.listener;

import com.microservices.notification_service.model.NotificationEvent;
import com.microservices.notification_service.service.NotificationSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationListener {

    private final NotificationSenderService senderService;

    // ✅ Listen to Order Events
    @KafkaListener(topics = "order-events", groupId = "notification-group", containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(
            @Payload NotificationEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, @Header(KafkaHeaders.OFFSET) long offset, Acknowledgment acknowledgment) {

        log.info("📬 Received order event from topic '{}' [partition: {}, offset: {}]: {}", topic, partition, offset, event);

        try {
            senderService.sendNotification(event);
            acknowledgment.acknowledge();
            log.info("✅ Order notification processed successfully");
        } catch (Exception e) {
            log.error("❌ Failed to process order notification: {}", e.getMessage(), e);
            // Don't acknowledge - message will be reprocessed
            throw e;
        }
    }

    // ✅ Listen to Delivery Events
    @KafkaListener(
            topics = "delivery-events",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeliveryEvent(@Payload NotificationEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, @Header(KafkaHeaders.OFFSET) long offset, Acknowledgment acknowledgment) {

        log.info("📬 Received delivery event from topic '{}' [partition: {}, offset: {}]: {}", topic, partition, offset, event);

        try {
            senderService.sendNotification(event);
            acknowledgment.acknowledge();
            log.info("✅ Delivery notification processed successfully");
        } catch (Exception e) {
            log.error("❌ Failed to process delivery notification: {}", e.getMessage(), e);
            throw e;
        }
    }
}