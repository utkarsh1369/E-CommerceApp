package com.microservice.product_service.controller;

import com.microservice.product_service.model.dto.ProductDTO;
import com.microservice.product_service.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ProductControllerUnitTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productDTO = ProductDTO.builder()
                .productId(1L)
                .productName("Laptop")
                .productDescription("Gaming Laptop")
                .productPrice(BigDecimal.valueOf(35000.00))
                .build();
    }

    @Test
    @DisplayName("Should create a new product and return 201 CREATED")
    void createProduct_ShouldReturnCreatedProduct() {
        when(productService.createProduct(any(ProductDTO.class))).thenReturn(productDTO);

        ResponseEntity<ProductDTO> response = productController.createProduct(productDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(productDTO, response.getBody());
        verify(productService, times(1)).createProduct(any(ProductDTO.class));
    }

    @Test
    @DisplayName("Should update existing product and return 200 OK")
    void updateProduct_ShouldReturnUpdatedProduct() {
        ProductDTO updated = ProductDTO.builder()
                .productId(1L)
                .productName("Updated Laptop")
                .productDescription("Updated Description")
                .productPrice(BigDecimal.valueOf(20000.00))
                .build();
        when(productService.updateProduct(anyLong(), any(ProductDTO.class))).thenReturn(updated);

        ResponseEntity<ProductDTO> response = productController.updateProduct(1L, updated);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updated, response.getBody());
        verify(productService, times(1)).updateProduct(anyLong(), any(ProductDTO.class));
    }

    @Test
    @DisplayName("Should return list of all products with 200 OK")
    void getAllProducts_ShouldReturnListOfProducts() {
        List<ProductDTO> productList = Arrays.asList(productDTO, new ProductDTO());
        when(productService.getAllProducts()).thenReturn(productList);

        ResponseEntity<List<ProductDTO>> response = productController.getAllProducts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productList, response.getBody());
        verify(productService, times(1)).getAllProducts();
    }

    @Test
    @DisplayName("Should return product by ID with 200 OK")
    void getProductById_ShouldReturnProduct() {
        when(productService.getProductById(anyLong())).thenReturn(productDTO);

        ResponseEntity<ProductDTO> response = productController.getProductById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productDTO, response.getBody());
        verify(productService, times(1)).getProductById(1L);
    }

    @Test
    @DisplayName("Should delete product and return 204 No Content")
    void deleteProductById_ShouldReturnNoContent() {
        doNothing().when(productService).deleteProductById(anyLong());

        ResponseEntity<Void> response = productController.deleteProductById(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(productService, times(1)).deleteProductById(1L);
    }
}
