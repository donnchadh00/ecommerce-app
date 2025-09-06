package com.ecommerce.order_service.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter; 

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "ecommerce.events";

    @Bean TopicExchange eventsExchange() { return new TopicExchange(EXCHANGE, true, false); }
    @Bean DirectExchange dlx() { return new DirectExchange("ecommerce.dlx"); }

    // payment.v1.authorized -> order-service
    @Bean Queue orderPaymentAuthorizedQ() {
        return QueueBuilder.durable("order.payment.authorized.q")
            .withArgument("x-dead-letter-exchange","ecommerce.dlx")
            .withArgument("x-dead-letter-routing-key","order.payment.authorized.dlq")
            .build();
    }
    @Bean Binding bindOrderPaymentAuthorized(TopicExchange eventsExchange, Queue orderPaymentAuthorizedQ) {
        return BindingBuilder.bind(orderPaymentAuthorizedQ).to(eventsExchange).with("payment.*.authorized");
    }
    @Bean Queue orderPaymentAuthorizedDlq() { return QueueBuilder.durable("order.payment.authorized.dlq").build(); }
    @Bean Binding bindOrderPaymentAuthorizedDlq(DirectExchange dlx, Queue orderPaymentAuthorizedDlq) {
        return BindingBuilder.bind(orderPaymentAuthorizedDlq).to(dlx).with("order.payment.authorized.dlq");
    }

    // payment.failed -> order-service
    @Bean Queue orderPaymentFailedQ() {
        return QueueBuilder.durable("order.payment.failed.q")
            .withArgument("x-dead-letter-exchange","ecommerce.dlx")
            .withArgument("x-dead-letter-routing-key","order.payment.failed.dlq")
            .build();
    }
    @Bean Binding bindOrderPaymentFailed(TopicExchange eventsExchange, Queue orderPaymentFailedQ) {
        return BindingBuilder.bind(orderPaymentFailedQ).to(eventsExchange).with("payment.*.failed");
    }
    @Bean Queue orderPaymentFailedDlq() { return QueueBuilder.durable("order.payment.failed.dlq").build(); }
    @Bean Binding bindOrderPaymentFailedDlq(DirectExchange dlx, Queue orderPaymentFailedDlq) {
        return BindingBuilder.bind(orderPaymentFailedDlq).to(dlx).with("order.payment.failed.dlq");
    }

    // inventory.rejected -> order-service
    @Bean Queue orderInventoryRejectedQ() {
        return QueueBuilder.durable("order.inventory.rejected.q")
            .withArgument("x-dead-letter-exchange","ecommerce.dlx")
            .withArgument("x-dead-letter-routing-key","order.inventory.rejected.dlq")
            .build();
    }
    @Bean Binding bindOrderInventoryRejected(TopicExchange eventsExchange, Queue orderInventoryRejectedQ) {
        return BindingBuilder.bind(orderInventoryRejectedQ).to(eventsExchange).with("inventory.*.rejected");
    }
    @Bean Queue orderInventoryRejectedDlq() { return QueueBuilder.durable("order.inventory.rejected.dlq").build(); }
    @Bean Binding bindOrderInventoryRejectedDlq(DirectExchange dlx, Queue orderInventoryRejectedDlq) {
        return BindingBuilder.bind(orderInventoryRejectedDlq).to(dlx).with("order.inventory.rejected.dlq");
    }

    // inventory.reserved -> order-service (to mark status=RESERVED)
    @Bean Queue orderInventoryReservedQ() {
        return QueueBuilder.durable("order.inventory.reserved.q")
            .withArgument("x-dead-letter-exchange","ecommerce.dlx")
            .withArgument("x-dead-letter-routing-key","order.inventory.reserved.dlq")
            .build();
    }
    @Bean Binding bindOrderInventoryReserved(TopicExchange eventsExchange, Queue orderInventoryReservedQ) {
        return BindingBuilder.bind(orderInventoryReservedQ).to(eventsExchange).with("inventory.*.reserved");
    }
    @Bean Queue orderInventoryReservedDlq() { return QueueBuilder.durable("order.inventory.reserved.dlq").build(); }
    @Bean Binding bindOrderInventoryReservedDlq(DirectExchange dlx, Queue orderInventoryReservedDlq) {
        return BindingBuilder.bind(orderInventoryReservedDlq).to(dlx).with("order.inventory.reserved.dlq");
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
