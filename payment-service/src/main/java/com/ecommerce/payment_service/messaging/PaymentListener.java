package com.ecommerce.payment_service.messaging;

import com.ecommerce.payment_service.model.Payment;
import com.ecommerce.payment_service.model.PaymentStatus;
import com.ecommerce.payment_service.outbox.EventOutbox;
import com.ecommerce.payment_service.repository.PaymentRepository;
import com.ecommerce.payment_service.service.StripePaymentService;

import com.ecommerce.events.Envelope;
import com.ecommerce.events.order.OrderPlaced;
import com.ecommerce.events.inventory.InventoryReserved;
import com.ecommerce.events.payment.PaymentCaptured;
import com.ecommerce.events.payment.PaymentFailed;

import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
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
  @RabbitListener(queues = "payment.order.placed.q")
  public void onOrderPlaced(Envelope<OrderPlaced> env, 
                            @Header(name="traceId", required=false) String traceId) {
    var order = env.data();
    Long orderId = parseLong(order.orderId());
    Long userId  = parseLong(order.userId());
    BigDecimal amount = order.total();
    String currency = "usd"; // Default to usd

    // Upsert a PENDING payment if absent
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

  // When inventory is reserved, actually charge. Emit captured/failed.
  @RabbitListener(queues = "payment.inventory.reserved.q")
  public void onInventoryReserved(Envelope<InventoryReserved> env,
                                  @Header(name="traceId", required=false) String traceId) throws Exception {
    Long orderId = parseLong(env.data().orderId());

    // Idempotent: already captured?
    if (paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCESSFUL)) {
      return;
    }

    // Prepare snapshot (ensure a row exists; read fields)
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

    // Validate amount outside tx
    if (snap.amount() == null || snap.amount().compareTo(BigDecimal.ZERO) <= 0) {
      // Mark failed
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
      PaymentIntent intent = withPermit(() -> stripe.createPaymentIntent(cents, currency));

      // Commit captured
      tx.execute(status -> {
        var p = paymentRepository.findById(snap.paymentId()).orElse(null);
        if (p != null && p.getStatus() != PaymentStatus.SUCCESSFUL) {
          p.setProviderPaymentId(intent.getId());
          p.setStatus(PaymentStatus.SUCCESSFUL);
          p.setUpdatedAt(LocalDateTime.now());
          paymentRepository.save(p);
        }
        return null;
      });

      // Outbox emit
      outbox.save(String.valueOf(orderId), "payment.v1.captured",
          new PaymentCaptured(String.valueOf(orderId), intent.getId(), snap.amount(), snap.currency()),
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
      return;

    } catch (StripeException se) {
      throw se;

    } catch (Exception e) {
      throw e;
    }
  }
}
