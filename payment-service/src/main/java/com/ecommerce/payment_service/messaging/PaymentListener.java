package com.ecommerce.payment_service.messaging;

import com.ecommerce.common.trace.ConsumerTraceSpan;
import com.ecommerce.payment_service.model.Payment;
import com.ecommerce.payment_service.model.PaymentStatus;
import com.ecommerce.payment_service.outbox.EventOutbox;
import com.ecommerce.payment_service.repository.PaymentRepository;
import com.ecommerce.payment_service.service.StripePaymentService;

import com.ecommerce.events.Envelope;
import com.ecommerce.events.order.OrderPlaced;
// import com.ecommerce.events.order.OrderConfirmed;
import com.ecommerce.events.order.OrderCancelled;
import com.ecommerce.events.inventory.InventoryReserved;
import com.ecommerce.events.payment.PaymentAuthorized;
import com.ecommerce.events.payment.PaymentFailed;
import com.ecommerce.events.payment.PaymentCaptured;

import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCaptureParams;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentListener {

  private final PaymentRepository paymentRepository;
  private final StripePaymentService stripe;
  private final EventOutbox outbox;

  private final TransactionTemplate tx;

  private final Semaphore stripePermits = new Semaphore(12);

  // Helpers

  private record Snap(Long paymentId, Long orderId, BigDecimal amount, String currency) {}

  private <T> T withPermit(Callable<T> task) throws Exception {
    stripePermits.acquire();
    try { return task.call(); }
    finally { stripePermits.release(); }
  }

  private Long parseLong(String s) {
    try { return Long.valueOf(s); } catch (Exception e) { return null; }
  }

  private String safeCode(String code) {
    return (code == null || code.isBlank()) ? "unknown" : code;
  }

  // OrderPlaced → stage a PENDING payment
  @RabbitListener(
    queues = "payment.order.placed.q",
    containerFactory = "fastListenerFactory"
  )
  public void onOrderPlaced(Envelope<OrderPlaced> env, 
                            @Header(name="traceId", required=false) String traceId,
                            @Header(name="traceparent", required=false) String traceparent) throws Exception {
    ConsumerTraceSpan.run(
        "payment-service",
        "payment.order.placed",
        "payment.order.placed.q",
        traceparent,
        traceId,
        () -> {
          var order = env.data();
          Long orderId = parseLong(order.orderId());
          Long userId  = parseLong(order.userId());
          BigDecimal amount = order.total();
          String currency = "usd";

          tx.execute(status -> {
            var existing = paymentRepository.findByOrderId(orderId).orElse(null);
            if (existing != null) return null;

            var p = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .currency(currency)
                .provider("stripe")
                .providerPaymentId(null)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            paymentRepository.save(p);
            return null;
          });
        }
    );
  }

  // When inventory is reserved, authorize only. Emit authorized/failed.
  @RabbitListener(
    queues = "payment.inventory.reserved.q",
    containerFactory = "stripeAuthListenerFactory"
  )
  public void onInventoryReserved(Envelope<InventoryReserved> env,
                                  @Header(name="traceId", required=false) String traceId,
                                  @Header(name="traceparent", required=false) String traceparent) throws Exception {
    ConsumerTraceSpan.run(
        "payment-service",
        "payment.inventory.reserved",
        "payment.inventory.reserved.q",
        traceparent,
        traceId,
        () -> {
          Long orderId = parseLong(env.data().orderId());

          if (paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.AUTHORIZED) ||
              paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCESSFUL)) {
            return;
          }

          Snap snap = tx.execute(status -> {
            Payment p = paymentRepository.findByOrderId(orderId).orElse(null);
            if (p == null) {
              p = Payment.builder()
                  .orderId(orderId)
                  .userId(null)
                  .amount(BigDecimal.ZERO)
                  .currency("usd")
                  .provider("stripe")
                  .providerPaymentId(null)
                  .status(PaymentStatus.PENDING)
                  .createdAt(LocalDateTime.now())
                  .updatedAt(LocalDateTime.now())
                  .build();
              p = paymentRepository.save(p);
            }
            return new Snap(p.getId(), p.getOrderId(), p.getAmount(), p.getCurrency());
          });

          if (snap.amount() == null || snap.amount().compareTo(BigDecimal.ZERO) <= 0) {
            tx.execute(status -> {
              var p = paymentRepository.findById(snap.paymentId()).orElse(null);
              if (p != null && p.getStatus() != PaymentStatus.SUCCESSFUL) {
                p.setStatus(PaymentStatus.FAILED);
                p.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(p);
              }
              return null;
            });
            outbox.save(String.valueOf(orderId), "payment.v1.failed",
                new PaymentFailed(String.valueOf(orderId), "invalid_amount"), traceId);
            return;
          }

          long cents = snap.amount().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
          String currency = snap.currency();

          try {
            String idempotencyKey = "order-" + orderId + "-auth";
            PaymentIntent intent = withPermit(() ->
                stripe.createAuthIntent(cents, currency, idempotencyKey, String.valueOf(orderId)));

            tx.execute(status -> {
              var p = paymentRepository.findById(snap.paymentId()).orElse(null);
              if (p != null && p.getStatus() != PaymentStatus.SUCCESSFUL) {
                p.setProviderPaymentId(intent.getId());
                p.setStatus(PaymentStatus.AUTHORIZED);
                p.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(p);
              }
              return null;
            });

            outbox.save(String.valueOf(orderId), "payment.v1.authorized",
                new PaymentAuthorized(String.valueOf(orderId), intent.getId(), snap.amount(), snap.currency()),
                traceId);

          } catch (CardException ce) {
            tx.execute(status -> {
              var p = paymentRepository.findById(snap.paymentId()).orElse(null);
              if (p != null && p.getStatus() != PaymentStatus.SUCCESSFUL) {
                p.setStatus(PaymentStatus.FAILED);
                p.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(p);
              }
              return null;
            });
            outbox.save(String.valueOf(orderId), "payment.v1.failed",
                new PaymentFailed(String.valueOf(orderId), "card_error:" + safeCode(ce.getCode())),
                traceId);
          }
        }
    );
  }

  @RabbitListener(
    queues = "payment.order.confirmed.q", 
    containerFactory = "stripeCaptureListenerFactory"
  )
  public void onOrderConfirmed(Envelope<com.ecommerce.events.order.OrderConfirmed> env,
                              @Header(name="traceId", required=false) String traceId,
                              @Header(name="traceparent", required=false) String traceparent) throws Exception {
    ConsumerTraceSpan.run(
        "payment-service",
        "payment.order.confirmed",
        "payment.order.confirmed.q",
        traceparent,
        traceId,
        () -> {
          Long orderId = Long.valueOf(env.data().orderId());

          String piId = tx.execute(s -> {
            var p = paymentRepository.findByOrderId(orderId).orElse(null);
            return (p == null) ? null : p.getProviderPaymentId();
          });
          if (piId == null || piId.isBlank()) {
            log.warn("capture skip: no providerPaymentId for order={}", orderId);
            return;
          }

          PaymentIntent captured = null;
          Exception last = null;
          for (int i = 0; i < 2; i++) {
            try {
              var pi = PaymentIntent.retrieve(piId);
              if (!"succeeded".equalsIgnoreCase(pi.getStatus())) {
                pi = pi.capture(PaymentIntentCaptureParams.builder().build());
              }
              captured = pi;
              break;
            } catch (Exception e) {
              last = e;
              Thread.sleep(200L * (i + 1));
            }
          }
          if (captured == null) {
            log.error("capture fail order={} pi={}", orderId, piId, last);
            throw last;
          }

          PaymentIntent finalCaptured = captured;
          tx.execute(s -> {
            var p = paymentRepository.findByOrderId(orderId).orElse(null);
            if (p != null && p.getStatus() != PaymentStatus.SUCCESSFUL) {
              p.setStatus(PaymentStatus.SUCCESSFUL);
              p.setUpdatedAt(LocalDateTime.now());
              paymentRepository.save(p);
            }
            return null;
          });

          outbox.save(String.valueOf(orderId), "payment.v1.captured",
              new PaymentCaptured(String.valueOf(orderId), finalCaptured.getId(), null, null), traceId);
        }
    );
  }

  // Void authorization if the order is cancelled before capture
  @RabbitListener(
    queues = "payment.order.cancelled.q",
    containerFactory = "fastListenerFactory"
  )
  public void onOrderCancelled(Envelope<OrderCancelled> env,
                              @Header(name="traceId", required=false) String traceId,
                              @Header(name="traceparent", required=false) String traceparent) throws Exception {
    ConsumerTraceSpan.run(
        "payment-service",
        "payment.order.cancelled",
        "payment.order.cancelled.q",
        traceparent,
        traceId,
        () -> {
          Long orderId = Long.valueOf(env.data().orderId());

          String piId = tx.execute(s -> {
            var p = paymentRepository.findByOrderId(orderId).orElse(null);
            return (p == null) ? null : p.getProviderPaymentId();
          });
          if (piId == null) return;

          PaymentIntent.retrieve(piId).cancel();

          tx.execute(s -> {
            var p = paymentRepository.findByOrderId(orderId).orElse(null);
            if (p != null && p.getStatus() != PaymentStatus.SUCCESSFUL) {
              p.setStatus(PaymentStatus.FAILED);
              p.setUpdatedAt(LocalDateTime.now());
              paymentRepository.save(p);
            }
            return null;
          });

          outbox.save(String.valueOf(orderId), "payment.v1.failed",
              new PaymentFailed(String.valueOf(orderId), "voided_on_cancel"),
              traceId);
        }
    );
  }

  @RabbitListener(queues = "payment.inventory.reserved.dlq")
  public void authDlq(byte[] body, @Headers java.util.Map<String,Object> h) {
    System.err.println("[DLQ auth] reason=" + h.get("x-first-death-reason")
        + " exc=" + h.get("x-exception-message"));
  }

  @RabbitListener(queues = "payment.order.confirmed.dlq")
  public void capDlq(byte[] body, @Headers java.util.Map<String,Object> h) {
    System.err.println("[DLQ capture] reason=" + h.get("x-first-death-reason")
        + " exc=" + h.get("x-exception-message"));
  }
}
