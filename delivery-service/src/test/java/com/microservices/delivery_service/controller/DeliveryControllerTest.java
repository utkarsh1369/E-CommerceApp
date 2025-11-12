package com.microservices.delivery_service.controller;

import com.microservices.delivery_service.model.Status;
import com.microservices.delivery_service.model.dto.DeliveryDto;
import com.microservices.delivery_service.model.dto.DeliveryRequestDto;
import com.microservices.delivery_service.model.dto.OrderDto;
import com.microservices.delivery_service.model.dto.UpdateDeliveryStatusDto;
import com.microservices.delivery_service.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeliveryControllerTest {

    @Mock
    private DeliveryService deliveryService;

    @InjectMocks
    private DeliveryController deliveryController;

    private DeliveryDto deliveryDto;
    private DeliveryRequestDto deliveryRequestDto;
    private UpdateDeliveryStatusDto updateStatusDto;
    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        deliveryDto = new DeliveryDto();
        deliveryDto.setDeliveryId(1L);
        deliveryDto.setStatus(Status.PENDING);

        deliveryRequestDto = new DeliveryRequestDto();
        deliveryRequestDto.setOrderId(100L);

        updateStatusDto = new UpdateDeliveryStatusDto();
        updateStatusDto.setStatus(Status.DELIVERED);

        orderDto = new OrderDto();
        orderDto.setOrderId(100L);
        orderDto.setUserId("user1");
    }

    @Test
    void createDelivery_ShouldReturnCreatedResponse() {
        when(deliveryService.createDelivery(deliveryRequestDto)).thenReturn(deliveryDto);

        ResponseEntity<DeliveryDto> response = deliveryController.createDelivery(deliveryRequestDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(deliveryDto, response.getBody());
        verify(deliveryService, times(1)).createDelivery(deliveryRequestDto);
    }

    @Test
    void updateDeliveryStatus_ShouldReturnUpdatedDelivery() {
        when(deliveryService.updateDeliveryStatus(1L, updateStatusDto)).thenReturn(deliveryDto);

        ResponseEntity<DeliveryDto> response = deliveryController.updateDeliveryStatus(1L, updateStatusDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(deliveryDto, response.getBody());
        verify(deliveryService).updateDeliveryStatus(1L, updateStatusDto);
    }

    @Test
    void getAllDeliveries_ShouldReturnListOfDeliveries() {
        when(deliveryService.findAllDeliveries()).thenReturn(List.of(deliveryDto));

        ResponseEntity<List<DeliveryDto>> response = deliveryController.getAllDeliveries();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(deliveryService).findAllDeliveries();
    }

    @Test
    void getDeliveryById_ShouldReturnDelivery() {
        when(deliveryService.findDeliveryById(1L)).thenReturn(deliveryDto);

        ResponseEntity<DeliveryDto> response = deliveryController.getDeliveryById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(deliveryDto, response.getBody());
        verify(deliveryService).findDeliveryById(1L);
    }

    @Test
    void getOrderByDeliveryId_ShouldReturnOrder() {
        when(deliveryService.findOrderByDeliveryId(1L)).thenReturn(orderDto);

        ResponseEntity<OrderDto> response = deliveryController.getOrderByDeliveryId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(orderDto, response.getBody());
        verify(deliveryService).findOrderByDeliveryId(1L);
    }
}

