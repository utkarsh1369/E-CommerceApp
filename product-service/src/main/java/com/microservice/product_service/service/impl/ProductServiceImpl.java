package com.microservice.product_service.service.impl;

import com.microservice.product_service.model.dto.ProductDTO;
import com.microservice.product_service.exception.ProductNotFoundException;
import com.microservice.product_service.mapper.ProductMapper;
import com.microservice.product_service.model.Product;
import com.microservice.product_service.repository.ProductRepository;
import com.microservice.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        log.info("Creating product: {}", productDTO.getProductName());

        Product product = productMapper.toEntity(productDTO);
        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully with ID: {}", savedProduct.getProductId());
        return productMapper.toDTO(savedProduct);
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        log.info("Updating product with ID: {}", productId);

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        productMapper.updateEntityFromDTO(productDTO, existingProduct);
        Product updatedProduct = productRepository.save(existingProduct);

        log.info("Product updated successfully with ID: {}", productId);
        return productMapper.toDTO(updatedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long productId) {
        log.info("Fetching product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return productMapper.toDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        log.info("Fetching all products");

        List<Product> products = productRepository.findAll();
        log.info("Found {} products", products.size());

        return productMapper.toDTOList(products);
    }

    @Override
    @Transactional
    public void deleteProductById(Long productId) {
        log.info("Deleting product with ID: {}", productId);

        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        productRepository.deleteById(productId);
        log.info("Product deleted successfully with ID: {}", productId);
    }
}