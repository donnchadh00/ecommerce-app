package com.ecommerce.payment_service.concurrency;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Component;

@Component
public class StripeLimiter {
  private final Semaphore permits = new Semaphore(12);

  public <T> T call(Callable<T> task) throws Exception {
    permits.acquire();
    try { return task.call(); }
    finally { permits.release(); }
  }
}
