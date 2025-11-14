package com.microservices.order_service.kafka.consumer;

import com.microservices.order_service.kafka.event.DeliveryCreatedEvent;
import com.microservices.order_service.kafka.event.DeliveryStatusChangedEvent;
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

    @KafkaListener(topics = "delivery-created", groupId = "${spring.kafka.consumer.group-id:order-service-group}",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void consumeDeliveryCreatedEvent(@Payload DeliveryCreatedEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, @Header(KafkaHeaders.OFFSET) long offset, Acknowledgment acknowledgment) {

        log.info("Received delivery created event from topic '{}' [partition: {}, offset: {}]: {}",
                topic, partition, offset, event);

        try {
            Orders order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found with ID: " + event.getOrderId()));

            order.setDeliveryId(event.getDeliveryId());
            orderRepository.save(order);

            log.info("Successfully updated order {} with delivery ID {}",
                    event.getOrderId(), event.getDeliveryId());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing delivery created event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "delivery-status-changed",
            groupId = "${spring.kafka.consumer.group-id:order-service-group}",
            containerFactory = "deliveryStatusKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeDeliveryStatusChanged(@Payload DeliveryStatusChangedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received delivery status change from topic '{}' [partition: {}, offset: {}]: deliveryId={}, status: {} → {}",
                topic, partition, offset, event.getDeliveryId(), event.getOldStatus(), event.getNewStatus());

        try {
            if ("DELIVERED".equals(event.getNewStatus())) {
                Orders order = orderRepository.findById(event.getOrderId())
                        .orElseThrow(() -> new RuntimeException("Order not found for status update: " + event.getOrderId()));

                order.setStatus(Status.DELIVERED);
                orderRepository.save(order);
                log.info("Order {} marked as DELIVERED", event.getOrderId());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing delivery status change: {}", e.getMessage(), e);
            throw e;
        }
    }
}