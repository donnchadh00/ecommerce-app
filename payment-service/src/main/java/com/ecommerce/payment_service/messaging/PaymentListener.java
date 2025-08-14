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

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PaymentListener {

  private final PaymentRepository paymentRepository;
  private final StripePaymentService stripe;
  private final EventOutbox outbox;

  // 1) Stage a PENDING payment when order is placed (so we have amount/currency/user)
  @Transactional
  @RabbitListener(queues = "payment.order.placed.q")
  public void onOrderPlaced(Envelope<OrderPlaced> env, 
                            @Header(name="traceId", required=false) String traceId) throws Exception {
    var order = env.data();

    Long orderId = parseLong(order.orderId());
    Long userId  = parseLong(order.userId());
    BigDecimal amount = order.total();
    String currency = "usd"; // If you include currency in OrderPlaced, use it; else default here.

    // Idempotent: if payment already exists, don't duplicate
    var existing = paymentRepository.findByOrderId(orderId).orElse(null);
    if (existing != null) return;

    var p = Payment.builder()
        .orderId(orderId)
        .userId(userId)
        .amount(amount)
        .currency(currency)
        .provider("stripe")
        .providerPaymentId(null) // set after capture
        .status(PaymentStatus.PENDING)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    paymentRepository.save(p);
  }

  // 2) When inventory is reserved, actually charge. Emit captured/failed.
  @Transactional
  @RabbitListener(queues = "payment.inventory.reserved.q")
  public void onInventoryReserved(Envelope<InventoryReserved> env, 
                                  @Header(name="traceId", required=false) String traceId) throws Exception {
    Long orderId = parseLong(env.data().orderId());

    // Idempotent: already captured?
    if (paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCESSFUL)) {
      return;
    }

    var payment = paymentRepository.findByOrderId(orderId).orElse(null);
    if (payment == null) {
      // Fallback: create a minimal pending record if we somehow missed OrderPlaced (eventual consistency)
      payment = Payment.builder()
          .orderId(orderId)
          .userId(null)
          .amount(BigDecimal.ZERO)
          .currency("usd")
          .provider("stripe")
          .status(PaymentStatus.PENDING)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();
      payment = paymentRepository.save(payment);
    }

    try {
      // Stripe expects cents
      long cents = payment.getAmount()
          .movePointRight(2)
          .setScale(0, RoundingMode.HALF_UP)
          .longValueExact();
      var intent = stripe.createPaymentIntent(cents, payment.getCurrency());

      payment.setProviderPaymentId(intent.getId());
      payment.setStatus(PaymentStatus.SUCCESSFUL);
      payment.setUpdatedAt(LocalDateTime.now());
      paymentRepository.save(payment);

      outbox.save(String.valueOf(orderId), "payment.v1.captured",
          new PaymentCaptured(String.valueOf(orderId), intent.getId(),
              payment.getAmount(), payment.getCurrency()),
          traceId);

    } catch (Exception e) {
      payment.setStatus(PaymentStatus.FAILED);
      payment.setUpdatedAt(LocalDateTime.now());
      paymentRepository.save(payment);

      outbox.save(String.valueOf(orderId), "payment.v1.failed",
          new PaymentFailed(String.valueOf(orderId), "processor_error"),
          traceId);

      // rethrow so Rabbit can retry, then DLQ if persistent failure
      throw e;
    }
  }

  private Long parseLong(String s) {
    try { return Long.valueOf(s); } catch (Exception e) { return null; }
  }
}
