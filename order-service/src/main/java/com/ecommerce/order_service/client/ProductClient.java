package com.ecommerce.order_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductClient {

  private final WebClient.Builder webClientBuilder;

  @Value("${product.service.url}")
  private String productServiceUrl;

  @Value("${product.internal.token}")
  private String internalToken;

  @Cacheable(
      value = "productPrices",
      key = "T(java.util.Objects).hash(#ids)",
      unless = "#result == null || #result.isEmpty()"
  )
  public Map<Long, BigDecimal> getPricesByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) return Map.of();

    String csv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));

    return webClientBuilder
        .baseUrl(productServiceUrl)
        .build()
        .get()
        .uri(uri -> uri.path("/api/products/prices").queryParam("ids", csv).build())
        .headers(h -> {
          if (!internalToken.isBlank()) h.add(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken);
          h.add(HttpHeaders.CONNECTION, "keep-alive");
        })
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<ProductPrice>>() {})
        .map(list -> list.stream().collect(Collectors.toMap(ProductPrice::id, ProductPrice::price)))
        .onErrorResume(e -> Mono.just(Map.of()))
        .timeout(Duration.ofSeconds(3))
        .block();
  }

  public record ProductPrice(Long id, BigDecimal price) {}
}
