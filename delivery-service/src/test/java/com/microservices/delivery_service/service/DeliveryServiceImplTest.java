package com.microservices.delivery_service.service;

import com.microservices.delivery_service.exception.*;
import com.microservices.delivery_service.feign.OrderClient;
import com.microservices.delivery_service.feign.UserClient;
import com.microservices.delivery_service.kafka.event.DeliveryCreatedEvent;
import com.microservices.delivery_service.kafka.event.DeliveryStatusChangedEvent;
import com.microservices.delivery_service.kafka.event.NotificationEvent;
import com.microservices.delivery_service.kafka.producer.DeliveryEventProducer;
import com.microservices.delivery_service.mapper.DeliveryMapper;
import com.microservices.delivery_service.model.Delivery;
import com.microservices.delivery_service.model.Status;
import com.microservices.delivery_service.model.dto.*;
import com.microservices.delivery_service.repository.DeliveryRepository;
import com.microservices.delivery_service.service.impl.DeliveryServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private OrderClient orderClient;
    @Mock private DeliveryMapper deliveryMapper;
    @Mock private DeliveryEventProducer deliveryEventProducer;
    @Mock private UserClient userClient;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    private Delivery delivery;
    private DeliveryDto deliveryDto;
    private DeliveryRequestDto requestDto;
    private UpdateDeliveryStatusDto statusDto;
    private OrderDto orderDto;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        delivery = new Delivery();
        delivery.setDeliveryId(1L);
        delivery.setOrderId(100L);
        delivery.setUserId("user1");
        delivery.setStatus(Status.PENDING);
        delivery.setCreatedAt(LocalDateTime.now());

        deliveryDto = new DeliveryDto();
        deliveryDto.setDeliveryId(1L);
        deliveryDto.setOrderId(100L);
        deliveryDto.setStatus(Status.PENDING);

        requestDto = new DeliveryRequestDto();
        requestDto.setOrderId(100L);
        requestDto.setUserId("user1");

        statusDto = new UpdateDeliveryStatusDto();
        statusDto.setStatus(Status.SHIPPED);

        orderDto = new OrderDto();
        orderDto.setOrderId(100L);

        userDto = new UserDto();
        userDto.setEmail("test@example.com");
    }

    @Test
    void createDelivery_ShouldCreateAndSendEvents() {
        when(orderClient.getOrderById(requestDto.getOrderId())).thenReturn(orderDto);
        when(deliveryMapper.toEntity(requestDto)).thenReturn(delivery);
        when(deliveryRepository.save(delivery)).thenReturn(delivery);
        when(userClient.getUserById("user1")).thenReturn(userDto);
        when(deliveryMapper.toDto(delivery)).thenReturn(deliveryDto);

        DeliveryDto result = deliveryService.createDelivery(requestDto);

        assertNotNull(result);
        assertEquals(1L, result.getDeliveryId());
        verify(deliveryRepository).save(delivery);
        verify(deliveryEventProducer).sendDeliveryCreatedEvent(any(DeliveryCreatedEvent.class));
        verify(deliveryEventProducer).sendNotification(any(NotificationEvent.class));
    }

    @Test
    void createDelivery_ShouldThrowOrderServiceException_WhenOrderClientFails() {
        when(orderClient.getOrderById(anyLong())).thenThrow(mock(FeignException.class));

        assertThrows(OrderServiceException.class, () -> deliveryService.createDelivery(requestDto));
    }

    @Test
    void updateDeliveryStatus_ShouldUpdateAndPublishEvents() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);
        when(deliveryMapper.toDto(any(Delivery.class))).thenReturn(deliveryDto);

        DeliveryDto result = deliveryService.updateDeliveryStatus(1L, statusDto);

        assertNotNull(result);
        verify(deliveryEventProducer).sendDeliveryStatusChangedEvent(any(DeliveryStatusChangedEvent.class));
        verify(deliveryEventProducer).sendNotification(any(NotificationEvent.class));
    }

    @Test
    void updateDeliveryStatus_ShouldThrow_WhenInvalidTransition() {
        delivery.setStatus(Status.DELIVERED);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class, () -> deliveryService.updateDeliveryStatus(1L, statusDto));
    }

    @Test
    void findDeliveryById_ShouldReturnDto() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryMapper.toDto(delivery)).thenReturn(deliveryDto);

        DeliveryDto result = deliveryService.findDeliveryById(1L);

        assertEquals(1L, result.getDeliveryId());
    }

    @Test
    void findAllDeliveries_ShouldReturnList() {
        when(deliveryRepository.findAll()).thenReturn(List.of(delivery));
        when(deliveryMapper.toDtoList(anyList())).thenReturn(List.of(deliveryDto));

        List<DeliveryDto> result = deliveryService.findAllDeliveries();

        assertEquals(1, result.size());
        verify(deliveryMapper).toDtoList(anyList());
    }

    @Test
    void findOrderByDeliveryId_ShouldReturnOrderDto() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(orderClient.getOrderById(100L)).thenReturn(orderDto);

        OrderDto result = deliveryService.findOrderByDeliveryId(1L);

        assertEquals(100L, result.getOrderId());
        verify(orderClient).getOrderById(100L);
    }

    @Test
    void findOrderByDeliveryId_ShouldThrow_WhenOrderServiceFails() {
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(orderClient.getOrderById(100L)).thenThrow(mock(FeignException.class));

        assertThrows(OrderServiceException.class, () -> deliveryService.findOrderByDeliveryId(1L));
    }

    @Test
    void findDeliveryById_ShouldThrow_WhenNotFound() {
        when(deliveryRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(DeliveryNotFoundException.class, () -> deliveryService.findDeliveryById(99L));
    }
}

