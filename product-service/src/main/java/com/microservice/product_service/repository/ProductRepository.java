package com.microservice.product_service.repository;

import com.microservice.product_service.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Custom query methods (optional, add as needed)
    Optional<Product> findByProductName(String productName);

    boolean existsByProductName(String productName);
}
