package com.ecommerce.payment_service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMessagingConfig {

  @Bean
  public Jackson2JsonMessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
    Jackson2JsonMessageConverter conv = new Jackson2JsonMessageConverter(objectMapper);
    DefaultJackson2JavaTypeMapper mapper = new DefaultJackson2JavaTypeMapper();
    mapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
    mapper.setTrustedPackages("com.ecommerce");
    conv.setJavaTypeMapper(mapper);
    conv.setAlwaysConvertToInferredType(true);
    return conv;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter conv) {
    RabbitTemplate tpl = new RabbitTemplate(cf);
    tpl.setMessageConverter(conv);
    return tpl;
  }

  @Bean(name = "stripeAuthListenerFactory")
  public SimpleRabbitListenerContainerFactory stripeAuthListenerFactory(
      ConnectionFactory cf, Jackson2JsonMessageConverter conv) {
    var f = new SimpleRabbitListenerContainerFactory();
    f.setConnectionFactory(cf);
    f.setMessageConverter(conv);
    f.setAcknowledgeMode(AcknowledgeMode.AUTO);
    f.setPrefetchCount(1);
    f.setConcurrentConsumers(18);
    f.setMaxConcurrentConsumers(36);
    f.setDefaultRequeueRejected(false);
    f.setMissingQueuesFatal(false);
    return f;
  }

  @Bean(name = "stripeCaptureListenerFactory")
  public SimpleRabbitListenerContainerFactory stripeCaptureListenerFactory(
      ConnectionFactory cf, Jackson2JsonMessageConverter conv) {
    var f = new SimpleRabbitListenerContainerFactory();
    f.setConnectionFactory(cf);
    f.setMessageConverter(conv);
    f.setAcknowledgeMode(AcknowledgeMode.AUTO);
    f.setPrefetchCount(1);
    f.setConcurrentConsumers(16);
    f.setMaxConcurrentConsumers(32);
    f.setDefaultRequeueRejected(false);
    f.setMissingQueuesFatal(false);
    return f;
  }

  @Bean(name = "fastListenerFactory")
  public SimpleRabbitListenerContainerFactory fastListenerFactory(
      ConnectionFactory cf, Jackson2JsonMessageConverter conv) {
    var f = new SimpleRabbitListenerContainerFactory();
    f.setConnectionFactory(cf);
    f.setMessageConverter(conv);
    f.setAcknowledgeMode(AcknowledgeMode.AUTO);
    f.setPrefetchCount(25);
    f.setConcurrentConsumers(8);
    f.setMaxConcurrentConsumers(16);
    f.setDefaultRequeueRejected(false);
    f.setMissingQueuesFatal(false);
    return f;
  }
}
