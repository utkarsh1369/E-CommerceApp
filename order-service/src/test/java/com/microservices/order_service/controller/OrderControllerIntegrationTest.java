package com.microservices.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.order_service.exception.OrderNotFoundException;
import com.microservices.order_service.model.dto.DeliveryDto;
import com.microservices.order_service.model.dto.OrderItemDto;
import com.microservices.order_service.model.dto.OrderRequestDto;
import com.microservices.order_service.model.dto.OrderResponseDto;
import com.microservices.order_service.service.OrderService;
import com.microservices.order_service.security.OrderSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
// Add these imports
import com.microservices.order_service.model.Status;
import com.microservices.order_service.model.PaymentMode;

@SuppressWarnings("FieldCanBeLocal")
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;
    @MockitoBean
    private OrderSecurityService orderSecurityService;

    private OrderRequestDto orderRequestDto;
    private OrderResponseDto orderResponseDto;
    private OrderResponseDto orderResponseDto2;
    private DeliveryDto deliveryDto;
    private List<OrderResponseDto> orderList;
    private OrderItemDto orderItemDto;

    @BeforeEach
    void setup() {
        orderItemDto = OrderItemDto.builder()
                .productId(1L)
                .quantity(2)
                .build();

        orderRequestDto = OrderRequestDto.builder()
                .userId("user123")
                .orderItems(List.of(orderItemDto))
                .paymentMode(PaymentMode.CREDIT_CARD)
                .build();

        orderResponseDto = OrderResponseDto.builder()
                .orderId(1L)
                .userId("user123")
                .orderAmount(new BigDecimal("200.00"))
                .orderStatus(Status.PENDING) // Changed from CREATED to PENDING
                .paymentMode(PaymentMode.CREDIT_CARD)
                .isPaid(false)
                .deliveryId(1L)
                .createdAt(LocalDateTime.now())
                .orderItems(List.of(orderItemDto))
                .build();

        orderResponseDto2 = OrderResponseDto.builder()
                .orderId(2L)
                .userId("user456")
                .orderAmount(new BigDecimal("50.00"))
                .orderStatus(Status.DELIVERED) // Changed from PAID to DELIVERED
                .build();

        orderList = List.of(orderResponseDto, orderResponseDto2);

        deliveryDto = DeliveryDto.builder()
                .deliveryId(1L)
                .orderId(1L)
                .userId("user123")
                .status(Status.PENDING)
                .build();
    }

    // ==================== POST /api/v1/orders ====================

    @Test
    @DisplayName("POST /orders - Success as USER")
    @WithMockUser(roles = "USER")
    void createOrder_asUser_returnsCreated() throws Exception {
        when(orderService.createOrder(any(OrderRequestDto.class))).thenReturn(orderResponseDto);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId", is(1)))
                .andExpect(jsonPath("$.userId", is("user123")));
    }

    @Test
    @DisplayName("POST /orders - Forbidden as ADMIN")
    @WithMockUser(roles = "ORDER_ADMIN")
    void createOrder_asAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /orders - Bad Request for invalid DTO")
    @WithMockUser(roles = "USER")
    void createOrder_invalidDto_returnsBadRequest() throws Exception {
        OrderRequestDto invalidDto = OrderRequestDto.builder()
                .userId(null) // Blank user ID
                .orderItems(Collections.emptyList()) // Empty items
                .paymentMode(null) // Null payment mode
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any());
    }

    // ==================== PUT /api/v1/orders/{orderId} ====================

    @Test
    @DisplayName("PUT /orders/{orderId} - Success as USER")
    @WithMockUser(roles = "USER")
    void updateOrder_asUser_returnsOk() throws Exception {
        when(orderService.updateOrder(eq(1L), any(OrderRequestDto.class))).thenReturn(orderResponseDto);

        mockMvc.perform(put("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", is(1)));
    }

    @Test
    @DisplayName("PUT /orders/{orderId} - Not Found")
    @WithMockUser(roles = "USER")
    void updateOrder_notFound_returnsNotFound() throws Exception {
        when(orderService.updateOrder(eq(99L), any(OrderRequestDto.class)))
                .thenThrow(new OrderNotFoundException("Order not found"));

        mockMvc.perform(put("/api/v1/orders/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequestDto)))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /api/v1/orders ====================

    @Test
    @DisplayName("GET /orders - Success as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void findAllOrders_asSuperAdmin_returnsOk() throws Exception {
        when(orderService.findAllOrders()).thenReturn(orderList);

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].orderId", is(1)));
    }

    @Test
    @DisplayName("GET /orders - Success as ORDER_ADMIN")
    @WithMockUser(roles = "ORDER_ADMIN")
    void findAllOrders_asOrderAdmin_returnsOk() throws Exception {
        when(orderService.findAllOrders()).thenReturn(orderList);

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /orders - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void findAllOrders_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/orders/{orderId} ====================

    @Test
    @DisplayName("GET /orders/{orderId} - Success as ADMIN")
    @WithMockUser(roles = "ORDER_ADMIN")
    void findOrderById_asAdmin_returnsOk() throws Exception {
        when(orderService.findOrderById(1L)).thenReturn(orderResponseDto);

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", is(1)));
    }

    @Test
    @DisplayName("GET /orders/{orderId} - Success as order owner")
    @WithMockUser(roles = "USER")
    void findOrderById_asOwner_returnsOk() throws Exception {
        // Mock the custom security check
        when(orderSecurityService.isOrderOwner(1L)).thenReturn(true);
        when(orderService.findOrderById(1L)).thenReturn(orderResponseDto);

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", is(1)));
    }

    @Test
    @DisplayName("GET /orders/{orderId} - Forbidden as other USER")
    @WithMockUser(roles = "USER")
    void findOrderById_asOtherUser_returnsForbidden() throws Exception {
        // Mock the custom security check to fail
        when(orderSecurityService.isOrderOwner(1L)).thenReturn(false);

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isForbidden());

        verify(orderService, never()).findOrderById(any());
    }

    @Test
    @DisplayName("GET /orders/{orderId} - Not Found")
    @WithMockUser(roles = "SUPER_ADMIN")
    void findOrderById_notFound_returnsNotFound() throws Exception {
        when(orderService.findOrderById(99L))
                .thenThrow(new OrderNotFoundException("Order not found"));

        mockMvc.perform(get("/api/v1/orders/99"))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /api/v1/orders/{orderId}/delivery ====================

    @Test
    @DisplayName("GET /orders/{orderId}/delivery - Success as DELIVERY_ADMIN")
    @WithMockUser(roles = "DELIVERY_ADMIN")
    void findDeliveryByOrderId_asDeliveryAdmin_returnsOk() throws Exception {
        when(orderService.getDeliveryByOrderId(1L)).thenReturn(deliveryDto);

        mockMvc.perform(get("/api/v1/orders/1/delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId", is(1)))
                .andExpect(jsonPath("$.orderId", is(1)));
    }

    @Test
    @DisplayName("GET /orders/{orderId}/delivery - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void findDeliveryByOrderId_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/orders/1/delivery"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /orders/{orderId}/delivery - Not Found")
    @WithMockUser(roles = "SUPER_ADMIN")
    void findDeliveryByOrderId_notFound_returnsNotFound() throws Exception {
        when(orderService.getDeliveryByOrderId(99L))
                .thenThrow(new OrderNotFoundException("Delivery not found for order 99"));

        mockMvc.perform(get("/api/v1/orders/99/delivery"))
                .andExpect(status().isNotFound());
    }
}
