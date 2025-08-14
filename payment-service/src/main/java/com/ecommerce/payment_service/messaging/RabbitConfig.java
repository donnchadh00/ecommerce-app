package com.ecommerce.payment_service.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@Configuration
public class RabbitConfig {
  public static final String EXCHANGE = "ecommerce.events";

  @Bean TopicExchange eventsExchange() { return new TopicExchange(EXCHANGE, true, false); }
  @Bean DirectExchange dlx() { return new DirectExchange("ecommerce.dlx"); }

  // Consume: order.v1.placed  (to stage a PENDING payment)
  @Bean Queue paymentOrderPlacedQ() {
    return QueueBuilder.durable("payment.order.placed.q")
        .withArgument("x-dead-letter-exchange","ecommerce.dlx")
        .withArgument("x-dead-letter-routing-key","payment.order.placed.dlq").build();
  }
  @Bean Binding bindPaymentOrderPlaced(TopicExchange eventsExchange, Queue paymentOrderPlacedQ) {
    return BindingBuilder.bind(paymentOrderPlacedQ)
        .to(eventsExchange)
        .with("order.*.placed");
  }
  @Bean Queue paymentOrderPlacedDlq() { return QueueBuilder.durable("payment.order.placed.dlq").build(); }
  @Bean Binding bindPaymentOrderPlacedDlq(DirectExchange dlx, Queue paymentOrderPlacedDlq) {
    return BindingBuilder.bind(paymentOrderPlacedDlq).to(dlx).with("payment.order.placed.dlq");
  }

  // Consume: inventory.v1.reserved (to actually charge)
  @Bean Queue paymentInventoryReservedQ() {
    return QueueBuilder.durable("payment.inventory.reserved.q")
        .withArgument("x-dead-letter-exchange","ecommerce.dlx")
        .withArgument("x-dead-letter-routing-key","payment.inventory.reserved.dlq").build();
  }
  @Bean Binding bindPaymentInventoryReserved(TopicExchange eventsExchange, Queue paymentInventoryReservedQ) {
    return BindingBuilder.bind(paymentInventoryReservedQ)
        .to(eventsExchange)
        .with("inventory.*.reserved");
  }
  @Bean Queue paymentInventoryReservedDlq() { return QueueBuilder.durable("payment.inventory.reserved.dlq").build(); }
  @Bean Binding bindPaymentInventoryReservedDlq(DirectExchange dlx, Queue paymentInventoryReservedDlq) {
    return BindingBuilder.bind(paymentInventoryReservedDlq).to(dlx).with("payment.inventory.reserved.dlq");
  }

  @Bean
  public MessageConverter messageConverter() {
      var conv = new Jackson2JsonMessageConverter();
      var mapper = (org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper) conv.getJavaTypeMapper();
      mapper.setTrustedPackages("com.ecommerce.*");
      return conv;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc) {
  RabbitTemplate tpl = new RabbitTemplate(cf);
  tpl.setMessageConverter(mc);
  return tpl;
  }
}
