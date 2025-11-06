package com.microservice.product_service.service;

import com.microservice.product_service.model.dto.ProductDTO;
import java.util.List;

public interface ProductService {

    ProductDTO createProduct(ProductDTO productDTO);
    ProductDTO updateProduct(Long productId, ProductDTO productDTO);
    ProductDTO getProductById(Long productId);
    List<ProductDTO> getAllProducts();
    void deleteProductById(Long productId);
}
