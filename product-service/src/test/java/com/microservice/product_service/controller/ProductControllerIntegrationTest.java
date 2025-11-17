package com.microservice.product_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.product_service.exception.ProductNotFoundException;
import com.microservice.product_service.model.dto.ProductDTO;
import com.microservice.product_service.service.ProductService;
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
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("FieldCanBeLocal")
public class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    private ProductDTO productDTO;
    private ProductDTO productDTO2;
    private List<ProductDTO> productList;

    @BeforeEach
    void setUp() {
        productDTO = ProductDTO.builder()
                .productId(1L)
                .productName("Laptop")
                .productDescription("A high-end gaming laptop")
                .productPrice(new BigDecimal("1500.00"))
                .build();
        productDTO2 = ProductDTO.builder()
                .productId(2L)
                .productName("Smartphone")
                .productDescription("The latest smartphone")
                .productPrice(new BigDecimal("800.00"))
                .build();
        productList = List.of(productDTO, productDTO2);
    }

    // ==================== POST /products/add ====================

    @Test
    @DisplayName("POST /api/v1/products/add - Success as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void createProduct_asSuperAdmin_returnsCreated() throws Exception {
        when(productService.createProduct(any(ProductDTO.class))).thenReturn(productDTO);

        mockMvc.perform(post("/api/v1/products/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName", is("Laptop")));
    }

    @Test
    @DisplayName("POST /api/v1/products/add - Success as PRODUCT_ADMIN")
    @WithMockUser(roles = "PRODUCT_ADMIN")
    void createProduct_asProductAdmin_returnsCreated() throws Exception {
        when(productService.createProduct(any(ProductDTO.class))).thenReturn(productDTO);

        mockMvc.perform(post("/api/v1/products/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/products/add - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void createProduct_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/products/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/products/add - Bad Request for invalid DTO")
    @WithMockUser(roles = "SUPER_ADMIN")
    void createProduct_invalidDTO_returnsBadRequest() throws Exception {
        // Assuming name is @NotBlank and price is @Positive
        ProductDTO invalidProduct = ProductDTO.builder()
                .productName("") // Invalid
                .productPrice(new BigDecimal("-10.00")) // Invalid
                .build();

        mockMvc.perform(post("/api/v1/products/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any());
    }

    @Test
    @DisplayName("POST /api/v1/products/add - Unauthorized")
    void createProduct_unauthorized_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/products/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== PUT /products/update/{productId} ====================

    @Test
    @DisplayName("PUT /api/v1/products/update/{productId} - Success as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void updateProduct_asSuperAdmin_returnsOk() throws Exception {
        when(productService.updateProduct(eq(1L), any(ProductDTO.class))).thenReturn(productDTO);

        mockMvc.perform(put("/api/v1/products/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.productName", is("Laptop")));
    }

    @Test
    @DisplayName("PUT /api/v1/products/update/{productId} - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void updateProduct_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/products/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/products/update/{productId} - Not Found")
    @WithMockUser(roles = "PRODUCT_ADMIN")
    void updateProduct_notFound_returnsNotFound() throws Exception {
        when(productService.updateProduct(eq(99L), any(ProductDTO.class)))
                .thenThrow(new ProductNotFoundException("Product not found"));

        mockMvc.perform(put("/api/v1/products/update/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isNotFound());
    }

    // ==================== GET /products ====================

    @Test
    @DisplayName("GET /api/v1/products - Success as USER")
    @WithMockUser(roles = "USER")
    void getAllProducts_asUser_returnsOk() throws Exception {
        when(productService.getAllProducts()).thenReturn(productList);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].productName", is("Laptop")));
    }

    @Test
    @DisplayName("GET /api/v1/products - Success as ORDER_ADMIN")
    @WithMockUser(roles = "ORDER_ADMIN")
    void getAllProducts_asOrderAdmin_returnsOk() throws Exception {
        when(productService.getAllProducts()).thenReturn(productList);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/v1/products - Unauthorized")
    void getAllProducts_unauthorized_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET /products/{productId} ====================

    @Test
    @DisplayName("GET /api/v1/products/{productId} - Success as USER")
    @WithMockUser(roles = "USER")
    void getProductById_asUser_returnsOk() throws Exception {
        when(productService.getProductById(1L)).thenReturn(productDTO);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName", is("Laptop")));
    }

    @Test
    @DisplayName("GET /api/v1/products/{productId} - Not Found")
    @WithMockUser(roles = "USER")
    void getProductById_notFound_returnsNotFound() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ProductNotFoundException("Product not found"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/products/{productId} - Unauthorized")
    void getProductById_unauthorized_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== DELETE /products/delete/{productId} ====================

    @Test
    @DisplayName("DELETE /api/v1/products/delete/{productId} - Success as SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void deleteProduct_asSuperAdmin_returnsNoContent() throws Exception {
        doNothing().when(productService).deleteProductById(1L);

        mockMvc.perform(delete("/api/v1/products/delete/1"))
                .andExpect(status().isNoContent());

        verify(productService, times(1)).deleteProductById(1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/products/delete/{productId} - Forbidden as USER")
    @WithMockUser(roles = "USER")
    void deleteProduct_asUser_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/products/delete/1"))
                .andExpect(status().isForbidden());

        verify(productService, never()).deleteProductById(any());
    }

    @Test
    @DisplayName("DELETE /api/v1/products/delete/{productId} - Not Found")
    @WithMockUser(roles = "PRODUCT_ADMIN")
    void deleteProduct_notFound_returnsNotFound() throws Exception {
        doThrow(new ProductNotFoundException("Product not found"))
                .when(productService).deleteProductById(99L);

        mockMvc.perform(delete("/api/v1/products/delete/99"))
                .andExpect(status().isNotFound());
    }
}
