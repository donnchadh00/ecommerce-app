package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.service.ProductService;
import com.ecommerce.product_service.service.ProductService.ProductPriceDto;
import com.ecommerce.product_service.service.ProductService.ProductSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAll());
    }

    @PreAuthorize("hasAnyRole('INTERNAL','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ProductSummaryDto> getById(@PathVariable Long id) {
        return ResponseEntity.of(productService.getProductById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.create(product));
    }

    @PreAuthorize("hasAnyRole('INTERNAL', 'ADMIN')")
    @GetMapping("/prices")
    public List<ProductPriceDto> getPrices(@RequestParam("ids") List<Long> ids) {
        return productService.getPricesByIds(ids);
    }
}
