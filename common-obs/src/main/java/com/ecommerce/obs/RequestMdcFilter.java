package com.ecommerce.obs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestMdcFilter extends OncePerRequestFilter {

  public static final String CORRELATION_HEADER = "X-Correlation-Id";

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String cid = req.getHeader(CORRELATION_HEADER);
    if (cid == null || cid.isBlank()) {
      cid = UUID.randomUUID().toString();
    }
    MDC.put("correlationId", cid);
    MDC.put("method", req.getMethod());
    MDC.put("uri", req.getRequestURI());
    res.setHeader(CORRELATION_HEADER, cid);

    try {
      chain.doFilter(req, res);
    } finally {
      MDC.clear();
    }
  }
}
