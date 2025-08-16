package com.ecommerce.cart_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.HttpHeaders;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${product.internal.token}")
    private String internalToken;

    public Optional<ProductDto> getProduct(Long productId) {
    return webClientBuilder.build()
        .get()
        .uri(productServiceUrl + "/api/products/{id}", productId)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
        .retrieve()
        .bodyToMono(ProductDto.class)
        .map(Optional::of)
        .onErrorResume(WebClientResponseException.NotFound.class,
                       e -> Mono.just(Optional.<ProductDto>empty()))
        .block();
}

    public record ProductDto(Long id, String name, String currency, String status) {}
}
