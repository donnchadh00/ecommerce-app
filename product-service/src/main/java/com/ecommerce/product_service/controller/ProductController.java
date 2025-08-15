package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productRepository.save(product));
    }

    @PreAuthorize("hasAnyRole('INTERNAL', 'ADMIN')")
    @GetMapping("/prices")
    public List<ProductPriceDto> getPrices(@RequestParam("ids") List<Long> ids) {
        return productRepository.findAllById(ids).stream()
                .map(p -> new ProductPriceDto(p.getId(), p.getPrice()))
                .toList();
    }
    public record ProductPriceDto(Long id, BigDecimal price) {}
}
