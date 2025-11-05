package com.microservices.order_service.kafka.consumer;

import com.microservices.order_service.kafka.event.DeliveryCreatedEvent;
import com.microservices.order_service.model.Orders;
import com.microservices.order_service.model.Status;
import com.microservices.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class DeliveryEventConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "delivery-created",
            groupId = "${spring.kafka.consumer.group-id:order-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeDeliveryCreatedEvent(@Payload DeliveryCreatedEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, @Header(KafkaHeaders.OFFSET) long offset, Acknowledgment acknowledgment) {

        log.info("Received delivery created event from topic '{}' [partition: {}, offset: {}]: {}",
                topic, partition, offset, event);

        try {
            // Find the order
            Orders order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found with ID: " + event.getOrderId()));

            // Update order with delivery ID
            order.setDeliveryId(event.getDeliveryId());
            orderRepository.save(order);

            log.info("Successfully updated order {} with delivery ID {}",
                    event.getOrderId(), event.getDeliveryId());

            // Acknowledge the message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing delivery created event: {}", e.getMessage(), e);
            // Don't acknowledge - message will be reprocessed
            throw e;
        }
    }

//    @KafkaListener(topics = "delivery-status-changed", groupId = "order-service-group")
//    public void consumeDeliveryStatusChanged(DeliveryStatusChangedEvent event) {
//        log.info("Delivery status changed: deliveryId={}, status: {} → {}",
//                event.getDeliveryId(), event.getOldStatus(), event.getNewStatus());
//
//        // Update order status based on delivery status
//        if ("DELIVERED".equals(event.getNewStatus())) {
//            Orders order = orderRepository.findById(event.getOrderId()).orElseThrow();
//            order.setStatus(Status.DELIVERED);
//            orderRepository.save(order);
//            log.info("Order {} marked as DELIVERED", event.getOrderId());
//        }
//    }
}