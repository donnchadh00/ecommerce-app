package com.ecommerce.product_service.service;

import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @Transactional
    public Product create(Product product) {
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ProductPriceDto> getPricesByIds(List<Long> ids) {
        return productRepository.findAllById(ids).stream()
                .map(p -> new ProductPriceDto(p.getId(), p.getPrice()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProductSummaryDto> getProductById(Long id) {
        return productRepository.findById(id)
            .map(p -> new ProductSummaryDto(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                "usd",
                "ACTIVE"
            ));
    }

    public record ProductPriceDto(Long id, BigDecimal price) {}

    public record ProductSummaryDto(
            Long id,
            String name,
            String description,
            BigDecimal price,
            String currency,
            String status // e.g., "ACTIVE" | "INACTIVE"
    ) {}
}
