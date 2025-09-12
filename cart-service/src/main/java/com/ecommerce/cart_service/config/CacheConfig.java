package com.ecommerce.cart_service.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.*;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {
  @Bean
  public CacheManager cacheManager() {
    var mgr = new CaffeineCacheManager();
    mgr.setCaffeine(Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofSeconds(60)));
    return mgr;
  }
}
