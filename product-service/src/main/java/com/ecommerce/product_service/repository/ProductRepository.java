package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.model.Product;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> getProductById(Long id);
}
