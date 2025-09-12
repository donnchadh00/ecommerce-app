package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.service.ProductService;
import com.ecommerce.product_service.service.ProductService.ProductPriceDto;
import com.ecommerce.product_service.service.ProductService.ProductSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.Duration;
import java.util.List;
import java.util.Map;

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
        return productService.getProductById(id)
            .map(dto -> {
                var etag = productService.computeEtag(dto);
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                    .eTag(etag)
                    .body(dto);
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PreAuthorize("hasAnyRole('INTERNAL','ADMIN')")
    @GetMapping("/bulk")
    public ResponseEntity<Map<Long, ProductSummaryDto>> getBulk(@RequestParam("ids") List<Long> ids) {
        var result = productService.getSummariesByIds(ids);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
            .body(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.create(product));
    }

    @PreAuthorize("hasAnyRole('INTERNAL', 'ADMIN')")
    @GetMapping("/prices")
    public ResponseEntity<List<ProductPriceDto>> getPrices(@RequestParam("ids") List<Long> ids) {
        var result = productService.getPricesByIds(ids);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
            .body(result);
    }
}
