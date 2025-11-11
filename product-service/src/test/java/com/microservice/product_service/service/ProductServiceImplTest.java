package com.microservice.product_service.service;

import com.microservice.product_service.exception.ProductNotFoundException;
import com.microservice.product_service.mapper.ProductMapper;
import com.microservice.product_service.model.Product;
import com.microservice.product_service.model.dto.ProductDTO;
import com.microservice.product_service.repository.ProductRepository;
import com.microservice.product_service.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        product = Product.builder()
                .productId(1L)
                .productName("Laptop")
                .productDescription("Gaming Laptop")
                .productPrice(BigDecimal.valueOf(12000.00))
                .build();

        productDTO = ProductDTO.builder()
                .productId(1L)
                .productName("Laptop")
                .productDescription("Gaming Laptop")
                .productPrice(BigDecimal.valueOf(12000.00))
                .build();
    }

    @Test
    @DisplayName("Should create product successfully")
    void createProduct_ShouldReturnCreatedProductDTO() {
        when(productMapper.toEntity(any(ProductDTO.class))).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toDTO(any(Product.class))).thenReturn(productDTO);

        ProductDTO result = productService.createProduct(productDTO);

        assertNotNull(result);
        assertEquals(productDTO.getProductName(), result.getProductName());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should update product successfully")
    void updateProduct_ShouldReturnUpdatedProductDTO() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
        doNothing().when(productMapper).updateEntityFromDTO(any(ProductDTO.class), any(Product.class));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toDTO(any(Product.class))).thenReturn(productDTO);

        ProductDTO result = productService.updateProduct(1L, productDTO);

        assertNotNull(result);
        assertEquals(productDTO.getProductId(), result.getProductId());
        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should throw when updating non-existing product")
    void updateProduct_ShouldThrowWhenNotFound() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.updateProduct(999L, productDTO));

        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should get product by ID successfully")
    void getProductById_ShouldReturnProductDTO() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
        when(productMapper.toDTO(any(Product.class))).thenReturn(productDTO);

        ProductDTO result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals(productDTO.getProductName(), result.getProductName());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw when product not found by ID")
    void getProductById_ShouldThrowWhenNotFound() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductById(404L));

        verify(productRepository, times(1)).findById(404L);
    }

    @Test
    @DisplayName("Should return list of all products")
    void getAllProducts_ShouldReturnProductList() {
        List<Product> products = Collections.singletonList(product);
        List<ProductDTO> productDTOs = Collections.singletonList(productDTO);

        when(productRepository.findAll()).thenReturn(products);
        when(productMapper.toDTOList(products)).thenReturn(productDTOs);

        List<ProductDTO> result = productService.getAllProducts();

        assertEquals(1, result.size());
        assertEquals(productDTO.getProductName(), result.getFirst().getProductName());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should delete product successfully")
    void deleteProductById_ShouldDeleteWhenExists() {
        when(productRepository.existsById(anyLong())).thenReturn(true);
        doNothing().when(productRepository).deleteById(anyLong());

        productService.deleteProductById(1L);

        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw when deleting non-existing product")
    void deleteProductById_ShouldThrowWhenNotFound() {
        when(productRepository.existsById(anyLong())).thenReturn(false);

        assertThrows(ProductNotFoundException.class,
                () -> productService.deleteProductById(123L));

        verify(productRepository, never()).deleteById(anyLong());
    }
}

