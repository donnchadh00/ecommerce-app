package com.ecommerce.product_service.service;

import com.ecommerce.product_service.model.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

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
        if (ids == null || ids.isEmpty()) return List.of();
        return productRepository.findAllById(ids).stream()
                .map(p -> new ProductPriceDto(p.getId(), p.getPrice()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProductSummaryDto> getProductById(Long id) {
        return productRepository.findById(id)
            .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Map<Long, ProductSummaryDto> getSummariesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return productRepository.findAllById(ids).stream()
            .map(this::toSummary)
            .collect(Collectors.toMap(ProductSummaryDto::id, s -> s));
    }

    private ProductSummaryDto toSummary(Product p) {
        return new ProductSummaryDto(
            p.getId(),
            p.getName(),
            p.getDescription(),
            p.getPrice(),
            "usd",
            "ACTIVE"
        );
    }

    public String computeEtag(ProductSummaryDto dto) {
        try {
            String base = dto.id() + "|" + dto.name() + "|" + dto.price() + "|" + dto.status();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] dig = md.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("\"");
            for (byte b : dig) sb.append(String.format("%02x", b));
            sb.append("\"");
            return sb.toString();
        } catch (Exception e) {
            return "\"" + Objects.hash(dto.id(), dto.name(), dto.price(), dto.status()) + "\"";
        }
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
