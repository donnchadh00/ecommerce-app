package com.ecommerce.inventory_service.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "ecommerce.events";

    @Bean TopicExchange eventsExchange() { return new TopicExchange(EXCHANGE, true, false); }
    @Bean DirectExchange dlx() { return new DirectExchange("ecommerce.dlx"); }

    // Consume order.v1.placed
    @Bean Queue inventoryOrderPlacedQ() {
        return QueueBuilder.durable("inventory.order.placed.q")
            .withArgument("x-dead-letter-exchange","ecommerce.dlx")
            .withArgument("x-dead-letter-routing-key","inventory.order.placed.dlq").build();
    }
    @Bean Binding bindInventoryOrderPlaced(TopicExchange eventsExchange, Queue inventoryOrderPlacedQ) {
        return BindingBuilder.bind(inventoryOrderPlacedQ).to(eventsExchange).with("order.*.placed");
    }
    @Bean Queue inventoryOrderPlacedDlq() { return QueueBuilder.durable("inventory.order.placed.dlq").build(); }
    @Bean Binding bindInventoryOrderPlacedDlq(DirectExchange dlx, Queue inventoryOrderPlacedDlq) {
        return BindingBuilder.bind(inventoryOrderPlacedDlq).to(dlx).with("inventory.order.placed.dlq");
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
