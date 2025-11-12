package com.microservices.order_service.service;

import com.microservices.order_service.exception.*;
import com.microservices.order_service.feign.DeliveryClient;
import com.microservices.order_service.feign.ProductClient;
import com.microservices.order_service.kafka.event.NotificationEvent;
import com.microservices.order_service.kafka.producer.NotificationEventProducer;
import com.microservices.order_service.mapper.OrderMapper;
import com.microservices.order_service.model.*;
import com.microservices.order_service.model.dto.*;
import com.microservices.order_service.repository.OrderRepository;
import com.microservices.order_service.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductClient productClient;
    @Mock private DeliveryClient deliveryClient;
    @Mock private OrderMapper orderMapper;
    @Mock private NotificationEventProducer notificationEventProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderRequestDto orderRequest;
    private Orders order;
    private OrderResponseDto responseDto;
    private ProductDto productDto;
    private OrderItemDto itemDto;

    @BeforeEach
    void setUp() {
        itemDto = new OrderItemDto();
        itemDto.setProductId(1L);
        itemDto.setQuantity(2);

        productDto = new ProductDto();
        productDto.setProductId(1L);
        productDto.setProductPrice(new BigDecimal("100.00"));

        orderRequest = new OrderRequestDto();
        orderRequest.setUserId("user123");
        orderRequest.setPaymentMode(PaymentMode.UPI);
        orderRequest.setOrderItems(List.of(itemDto));

        order = Orders.builder()
                .orderId(1L)
                .userId("user123")
                .orderAmount(new BigDecimal("200.00"))
                .status(Status.PENDING)
                .isPaid(false)
                .build();

        responseDto = new OrderResponseDto();
        responseDto.setOrderId(1L);
        responseDto.setOrderStatus(Status.PENDING);
    }

    @Test
    void createOrder_ShouldCalculateAmount_SaveOrder_AndSendNotification() {
        when(productClient.getProductById(1L)).thenReturn(productDto);
        when(orderMapper.toOrderItem(any(), any())).thenReturn(new OrderItem());
        when(orderRepository.save(any(Orders.class))).thenReturn(order);
        when(orderMapper.toResponseDto(order)).thenReturn(responseDto);

        OrderResponseDto result = orderService.createOrder(orderRequest);

        assertThat(result).isEqualTo(responseDto);
        verify(orderRepository).save(any(Orders.class));
        verify(notificationEventProducer).sendNotification(any(NotificationEvent.class));
    }

    @Test
    void createOrder_ShouldThrow_WhenProductFetchFails() {
        when(productClient.getProductById(1L)).thenThrow(new ProductServiceException("fail"));

        assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                .isInstanceOf(ProductServiceException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrder_ShouldUpdateExistingOrderAndSendNotification() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productClient.getProductById(1L)).thenReturn(productDto);
        when(orderMapper.toOrderItem(any(), any())).thenReturn(new OrderItem());
        when(orderRepository.save(any())).thenReturn(order);
        when(orderMapper.toResponseDto(order)).thenReturn(responseDto);

        OrderResponseDto result = orderService.updateOrder(1L, orderRequest);

        assertThat(result).isEqualTo(responseDto);
        verify(orderRepository).save(any(Orders.class));
        verify(notificationEventProducer).sendNotification(any(NotificationEvent.class));
    }

    @Test
    void updateOrder_ShouldThrow_WhenOrderNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.updateOrder(1L, orderRequest))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void findOrderById_ShouldReturnResponseDto() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponseDto(order)).thenReturn(responseDto);

        OrderResponseDto result = orderService.findOrderById(1L);

        assertThat(result).isEqualTo(responseDto);
        verify(orderRepository).findById(1L);
    }

    @Test
    void findOrderById_ShouldThrow_WhenNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.findOrderById(1L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void findAllOrders_ShouldReturnMappedList() {
        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(orderMapper.toResponseDtoList(List.of(order))).thenReturn(List.of(responseDto));

        List<OrderResponseDto> result = orderService.findAllOrders();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(responseDto);
        verify(orderRepository).findAll();
    }

    @Test
    void getDeliveryByOrderId_ShouldReturnDelivery() {
        DeliveryDto deliveryDto = new DeliveryDto();
        deliveryDto.setOrderId(1L);
        deliveryDto.setStatus(Status.SHIPPED);
        deliveryDto.setExpectedDeliveryDate(LocalDate.now());

        order.setDeliveryId(10L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(deliveryClient.getDeliveryById(10L)).thenReturn(deliveryDto);

        DeliveryDto result = orderService.getDeliveryByOrderId(1L);

        assertThat(result).isEqualTo(deliveryDto);
    }

    @Test
    void getDeliveryByOrderId_ShouldThrow_WhenOrderNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getDeliveryByOrderId(1L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getDeliveryByOrderId_ShouldThrow_WhenDeliveryIdMissing() {
        order.setDeliveryId(null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getDeliveryByOrderId(1L))
                .isInstanceOf(DeliveryNotFoundException.class);
    }
}