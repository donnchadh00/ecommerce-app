package com.ecommerce.obs;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ClientCorrelationConfig {

  /** Adds X-Correlation-Id from MDC to all WebClient requests (if present). */
  @Bean
  @ConditionalOnClass(WebClient.class)
  @ConditionalOnMissingBean(name = "correlationIdExchangeFilter")
  public ExchangeFilterFunction correlationIdExchangeFilter() {
    return (request, next) -> {
      String cid = MDC.get("correlationId");
      ClientRequest mutated = (cid != null && !cid.isBlank())
          ? ClientRequest.from(request)
              .headers(h -> h.set(RequestMdcFilter.CORRELATION_HEADER, cid))
              .build()
          : request;
      return next.exchange(mutated);
    };
  }

  /** Auto-applies the filter to any injected WebClient.Builder beans. */
  @Bean
  @ConditionalOnClass(WebClient.Builder.class)
  public WebClientCustomizer webClientCustomizer(ExchangeFilterFunction correlationIdExchangeFilter) {
    return builder -> builder.filter(correlationIdExchangeFilter);
  }

  /** Adds X-Correlation-Id from MDC to RestTemplate requests. */
  @Bean
  @ConditionalOnClass(RestTemplate.class)
  public RestTemplateCustomizer restTemplateCustomizer() {
    return (RestTemplate rt) -> {
      ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
        String cid = MDC.get("correlationId");
        if (cid != null && !cid.isBlank()) {
          request.getHeaders().set(RequestMdcFilter.CORRELATION_HEADER, cid);
        }
        return execution.execute(request, body);
      };
      rt.getInterceptors().add(interceptor);
    };
  }
}
