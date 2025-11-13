package com.microservices.delivery_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.delivery_service.exception.DeliveryNotFoundException;
import com.microservices.delivery_service.model.Status;
import com.microservices.delivery_service.model.dto.DeliveryDto;
import com.microservices.delivery_service.model.dto.DeliveryRequestDto;
import com.microservices.delivery_service.model.dto.OrderDto;
import com.microservices.delivery_service.model.dto.UpdateDeliveryStatusDto;
import com.microservices.delivery_service.security.DeliverySecurityService;
import com.microservices.delivery_service.service.DeliveryService;
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
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("FieldCanBeLocal")
@SpringBootTest
@AutoConfigureMockMvc
class DeliveryControllerIntegrationTest {


    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryService deliveryService;
    @MockitoBean
    private DeliverySecurityService deliverySecurityService;

    private DeliveryDto deliveryDto;
    private DeliveryDto deliveryDto2;
    private DeliveryRequestDto deliveryRequestDto;
    private UpdateDeliveryStatusDto updateStatusDto;
    private OrderDto orderDto;
    private List<DeliveryDto> allDeliveries;

    @BeforeEach
    void setup() {
        deliveryDto = DeliveryDto.builder()
                .deliveryId(1L)
                .orderId(100L)
                .userId("deliveryUser123")
                .status(Status.PENDING)
                .expectedDeliveryDate(LocalDate.now().plusDays(3))
                .build();

        deliveryDto2 = DeliveryDto.builder()
                .deliveryId(2L)
                .orderId(101L)
                .userId("deliveryUser456")
                .status(Status.SHIPPED)
                .build();

        allDeliveries = List.of(deliveryDto, deliveryDto2);

        deliveryRequestDto = DeliveryRequestDto.builder()
                .orderId(100L)
                .userId("deliveryUser123")
                .build();

        updateStatusDto = UpdateDeliveryStatusDto.builder()
                .status(Status.SHIPPED)
                .build();

        orderDto = OrderDto.builder()
                .orderId(100L)
                .userId("deliveryUser123")
                .orderAmount(new BigDecimal("199.99"))
                .orderStatus(Status.PENDING)
                .build();
    }

    // ==================== POST /delivery/create ====================

    @Test
    @DisplayName("POST /delivery/create - Success as DELIVERY_ADMIN")
    @WithMockUser(roles = "DELIVERY_ADMIN")
    void createDelivery_asDeliveryAdmin_returnsCreated() throws Exception {
        when(deliveryService.createDelivery(any(DeliveryRequestDto.class))).thenReturn(deliveryDto);

        mockMvc.perform(post("/delivery/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deliveryRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryId", is(1)))
                .andExpect(jsonPath("$.orderId", is(100)));
    }

    @Test
    @DisplayName("POST /delivery/create - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void createDelivery_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(post("/delivery/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deliveryRequestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /delivery/create - Bad Request for invalid DTO")
    @WithMockUser(roles = "SUPER_ADMIN")
    void createDelivery_invalidDto_returnsBadRequest() throws Exception {
        DeliveryRequestDto invalidDto = DeliveryRequestDto.builder().orderId(null).build();

        mockMvc.perform(post("/delivery/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(deliveryService, never()).createDelivery(any());
    }

    // ==================== PATCH /delivery/{deliveryId}/update-status ====================

    @Test
    @DisplayName("PATCH /update-status - Success as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void updateDeliveryStatus_asSuperAdmin_returnsOk() throws Exception {
        DeliveryDto shippedDelivery = DeliveryDto.builder()
                .deliveryId(1L)
                .orderId(100L)
                .userId("deliveryUser123")
                .status(Status.SHIPPED) // The updated status
                .build();

        when(deliveryService.updateDeliveryStatus(eq(1L), any(UpdateDeliveryStatusDto.class)))
                .thenReturn(shippedDelivery);

        mockMvc.perform(patch("/delivery/1/update-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SHIPPED")));
    }

    @Test
    @DisplayName("PATCH /update-status - Bad Request for invalid Status DTO")
    @WithMockUser(roles = "DELIVERY_ADMIN")
    void updateDeliveryStatus_invalidDto_returnsBadRequest() throws Exception {
        UpdateDeliveryStatusDto invalidDto = UpdateDeliveryStatusDto.builder().status(null).build();

        mockMvc.perform(patch("/delivery/1/update-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /update-status - Not Found")
    @WithMockUser(roles = "DELIVERY_ADMIN")
    void updateDeliveryStatus_notFound_returnsNotFound() throws Exception {
        when(deliveryService.updateDeliveryStatus(eq(99L), any(UpdateDeliveryStatusDto.class)))
                .thenThrow(new DeliveryNotFoundException("Delivery not found"));

        mockMvc.perform(patch("/delivery/99/update-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusDto)))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /delivery ====================

    @Test
    @DisplayName("GET /delivery - Success as DELIVERY_ADMIN")
    @WithMockUser(roles = "DELIVERY_ADMIN")
    void getAllDeliveries_asDeliveryAdmin_returnsOk() throws Exception {
        when(deliveryService.findAllDeliveries()).thenReturn(allDeliveries);

        mockMvc.perform(get("/delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].deliveryId", is(1)));
    }

    @Test
    @DisplayName("GET /delivery - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void getAllDeliveries_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/delivery"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /delivery/{deliveryId} ====================

    @Test
    @DisplayName("GET /{deliveryId} - Success as ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getDeliveryById_asAdmin_returnsOk() throws Exception {
        when(deliveryService.findDeliveryById(1L)).thenReturn(deliveryDto);

        mockMvc.perform(get("/delivery/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId", is(1)))
                .andExpect(jsonPath("$.userId", is("deliveryUser123")));
    }

    @Test
    @DisplayName("GET /{deliveryId} - Success as delivery owner")
    @WithMockUser(username = "deliveryUser123", roles = "USER") // Note: Role USER is not in @PreAuthorize
    void getDeliveryById_asOwner_returnsOk() throws Exception {
        // We must mock the security service
        when(deliverySecurityService.isDeliveryOwner(1L)).thenReturn(true);
        when(deliveryService.findDeliveryById(1L)).thenReturn(deliveryDto);

        mockMvc.perform(get("/delivery/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId", is(1)));
    }

    @Test
    @DisplayName("GET /{deliveryId} - Forbidden as other user")
    @WithMockUser(username = "notTheOwner", roles = "USER") // Note: Role USER is not in @PreAuthorize
    void getDeliveryById_asOtherUser_returnsForbidden() throws Exception {
        // Mock security service to return false
        when(deliverySecurityService.isDeliveryOwner(1L)).thenReturn(false);

        mockMvc.perform(get("/delivery/1"))
                .andExpect(status().isForbidden());

        verify(deliveryService, never()).findDeliveryById(any());
    }

    @Test
    @DisplayName("GET /{deliveryId} - Not Found")
    @WithMockUser(roles = "DELIVERY_ADMIN")
    void getDeliveryById_notFound_returnsNotFound() throws Exception {
        when(deliveryService.findDeliveryById(99L))
                .thenThrow(new DeliveryNotFoundException("Delivery not found"));

        mockMvc.perform(get("/delivery/99"))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /delivery/{deliveryId}/order ====================

    @Test
    @DisplayName("GET /{deliveryId}/order - Success as ORDER_ADMIN")
    @WithMockUser(roles = "ORDER_ADMIN")
    void getOrderByDeliveryId_asOrderAdmin_returnsOk() throws Exception {
        when(deliveryService.findOrderByDeliveryId(1L)).thenReturn(orderDto);

        mockMvc.perform(get("/delivery/1/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", is(100)));
    }

    @Test
    @DisplayName("GET /{deliveryId}/order - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void getOrderByDeliveryId_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/delivery/1/order"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /{deliveryId}/order - Not Found")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getOrderByDeliveryId_notFound_returnsNotFound() throws Exception {
        when(deliveryService.findOrderByDeliveryId(99L))
                .thenThrow(new DeliveryNotFoundException("Order not found for delivery 99"));

        mockMvc.perform(get("/delivery/99/order"))
                .andExpect(status().isNotFound());
    }
}