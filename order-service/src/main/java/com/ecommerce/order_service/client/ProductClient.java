package com.ecommerce.order_service.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import org.springframework.http.HttpHeaders;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductClient {

  private final WebClient.Builder webClientBuilder;

  @Value("${product.service.url}")
  private String productServiceUrl;

  @Value("${product.internal.token}")
  private String internalToken;

  // Returns a Map<productId, price>
  public Map<Long, BigDecimal> getPricesByIds(List<Long> ids) {
    String csv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    return webClientBuilder.build()
        .get()
        .uri(productServiceUrl + "/api/products/prices?ids={ids}", csv)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
        .retrieve()
        .bodyToFlux(ProductPrice.class)
        .collectMap(ProductPrice::id, ProductPrice::price)
        .block();
  }

  public record ProductPrice(Long id, BigDecimal price) {}
}
