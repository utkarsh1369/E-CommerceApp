package com.microservices.order_service.controller;

import com.microservices.order_service.model.Status;
import com.microservices.order_service.model.dto.DeliveryDto;
import com.microservices.order_service.model.dto.OrderRequestDto;
import com.microservices.order_service.model.dto.OrderResponseDto;
import com.microservices.order_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderControllerUnitTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private OrderRequestDto orderRequestDto;
    private OrderResponseDto orderResponseDto;
    private DeliveryDto deliveryDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        orderRequestDto = new OrderRequestDto();
        orderResponseDto = new OrderResponseDto();
        orderResponseDto.setOrderId(1L);
        orderResponseDto.setOrderStatus(Status.PENDING);

        deliveryDto = new DeliveryDto();
        deliveryDto.setOrderId(1L);
        deliveryDto.setStatus(Status.SHIPPED);
        deliveryDto.setExpectedDeliveryDate(LocalDate.now());
    }

    @Test
    void createOrder_ShouldReturnCreated() {
        when(orderService.createOrder(orderRequestDto)).thenReturn(orderResponseDto);

        ResponseEntity<OrderResponseDto> response = orderController.createOrder(orderRequestDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(orderResponseDto);
        verify(orderService).createOrder(orderRequestDto);
    }

    @Test
    void updateOrder_ShouldReturnOk() {
        when(orderService.updateOrder(1L, orderRequestDto)).thenReturn(orderResponseDto);

        ResponseEntity<OrderResponseDto> response = orderController.updateOrder(1L, orderRequestDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(orderResponseDto);
        verify(orderService).updateOrder(1L, orderRequestDto);
    }

    @Test
    void findAllOrders_ShouldReturnOkWithList() {
        when(orderService.findAllOrders()).thenReturn(List.of(orderResponseDto));

        ResponseEntity<List<OrderResponseDto>> response = orderController.findAllOrders();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst()).isEqualTo(orderResponseDto);
        verify(orderService).findAllOrders();
    }

    @Test
    void findOrderById_ShouldReturnOk() {
        when(orderService.findOrderById(1L)).thenReturn(orderResponseDto);

        ResponseEntity<OrderResponseDto> response = orderController.findOrderById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(orderResponseDto);
        verify(orderService).findOrderById(1L);
    }

    @Test
    void findDeliveryByOrderId_ShouldReturnOk() {
        when(orderService.getDeliveryByOrderId(1L)).thenReturn(deliveryDto);

        ResponseEntity<DeliveryDto> response = orderController.findDeliveryByOrderId(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(deliveryDto);
        verify(orderService).getDeliveryByOrderId(1L);
    }
}
