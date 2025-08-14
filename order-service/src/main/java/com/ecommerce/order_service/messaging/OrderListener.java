package com.ecommerce.order_service.messaging;

import com.ecommerce.order_service.repository.OrderRepository;
import com.ecommerce.order_service.outbox.EventOutbox;

import com.ecommerce.events.Envelope;
import com.ecommerce.events.inventory.InventoryRejected;
import com.ecommerce.events.inventory.InventoryReserved;
import com.ecommerce.events.order.OrderCancelled;
import com.ecommerce.events.order.OrderConfirmed;
import com.ecommerce.events.payment.PaymentCaptured;
import com.ecommerce.events.payment.PaymentFailed;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderListener {

    private final OrderRepository orderRepository;
    private final EventOutbox outbox;

    @Transactional
    @RabbitListener(queues = "order.payment.captured.q")
    public void onPaymentCaptured(Envelope<PaymentCaptured> env,
                                  @Header(name="traceId", required=false) String traceId) throws Exception {
        var orderId = env.data().orderId();
        var order = orderRepository.findById(Long.valueOf(orderId)).orElse(null);
        if (order == null) return;

        if (!"CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            order.setStatus("CONFIRMED");
            orderRepository.save(order);
            outbox.save(orderId, "order.v1.confirmed", new OrderConfirmed(orderId), traceId);
        }
    }

    @Transactional
    @RabbitListener(queues = "order.payment.failed.q")
    public void onPaymentFailed(Envelope<PaymentFailed> env,
                                @Header(name="traceId", required=false) String traceId) throws Exception {
        cancelIfNotFinal(env.data().orderId(), "payment_failed", traceId);
    }

    @Transactional
    @RabbitListener(queues = "order.inventory.rejected.q")
    public void onInventoryRejected(Envelope<InventoryRejected> env,
                                    @Header(name="traceId", required=false) String traceId) throws Exception {
        cancelIfNotFinal(env.data().orderId(), "insufficient_stock", traceId);
    }

    @Transactional
    @RabbitListener(queues = "order.inventory.reserved.q")
    public void onInventoryReserved(Envelope<InventoryReserved> env) throws Exception {
        var order = orderRepository.findById(Long.valueOf(env.data().orderId())).orElse(null);
        if (order == null) return;
        if (!"CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            order.setStatus("RESERVED");
            orderRepository.save(order);
        }
    }

    private void cancelIfNotFinal(String orderId, String reason, String traceId) {
        var order = orderRepository.findById(Long.valueOf(orderId)).orElse(null);
        if (order == null) return;
        if (!"CONFIRMED".equalsIgnoreCase(order.getStatus()) &&
            !"CANCELLED".equalsIgnoreCase(order.getStatus())) {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            outbox.save(orderId, "order.v1.cancelled", new OrderCancelled(orderId, reason), traceId);
        }
    }
}
