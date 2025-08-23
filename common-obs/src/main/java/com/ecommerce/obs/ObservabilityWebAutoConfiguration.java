package com.ecommerce.obs;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ObservabilityWebAutoConfiguration {

  // Always safe: no external framework types here
  @Bean
  public FilterRegistrationBean<RequestMdcFilter> requestMdcFilterRegistration() {
    FilterRegistrationBean<RequestMdcFilter> reg = new FilterRegistrationBean<>();
    reg.setFilter(new RequestMdcFilter());
    reg.setOrder(Integer.MIN_VALUE + 10);
    reg.addUrlPatterns("/*");
    return reg;
  }

  /** RestTemplate correlation: only active if RestTemplate is on classpath */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(RestTemplate.class)
  static class RestTemplateCorrelationConfiguration {

    @Bean
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

  /** WebClient correlation: only active if WebClient is on classpath */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(org.springframework.web.reactive.function.client.WebClient.class)
  static class WebClientCorrelationConfiguration {

    @Bean
    public org.springframework.web.reactive.function.client.ExchangeFilterFunction correlationIdExchangeFilter() {
      return (request, next) -> {
        String cid = MDC.get("correlationId");
        org.springframework.web.reactive.function.client.ClientRequest mutated =
            (cid != null && !cid.isBlank())
                ? org.springframework.web.reactive.function.client.ClientRequest.from(request)
                    .headers(h -> h.set(RequestMdcFilter.CORRELATION_HEADER, cid))
                    .build()
                : request;
        return next.exchange(mutated);
      };
    }

    @Bean
    @ConditionalOnClass(org.springframework.web.reactive.function.client.WebClient.Builder.class)
    public org.springframework.boot.web.reactive.function.client.WebClientCustomizer webClientCustomizer(
        org.springframework.web.reactive.function.client.ExchangeFilterFunction filter) {
      return builder -> builder.filter(filter);
    }
  }
}
