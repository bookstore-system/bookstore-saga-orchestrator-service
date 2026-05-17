package com.notfound.sagaorchestrator.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String COMMAND_EXCHANGE = "bookstore.commands";
    public static final String EVENT_EXCHANGE = "bookstore.events";
    public static final String CHECKOUT_EVENT_QUEUE = "saga.checkout.events.queue";

    @Bean
    public TopicExchange commandExchange() {
        return new TopicExchange(COMMAND_EXCHANGE);
    }

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE);
    }

    @Bean
    public Queue checkoutEventQueue() {
        return new Queue(CHECKOUT_EVENT_QUEUE, true);
    }

    @Bean
    public Declarables checkoutEventBindings(Queue checkoutEventQueue, TopicExchange eventExchange) {
        return new Declarables(
                BindingBuilder.bind(checkoutEventQueue).to(eventExchange).with("order.*"),
                BindingBuilder.bind(checkoutEventQueue).to(eventExchange).with("book.stock.*"),
                BindingBuilder.bind(checkoutEventQueue).to(eventExchange).with("promotion.*"),
                BindingBuilder.bind(checkoutEventQueue).to(eventExchange).with("payment.*"),
                BindingBuilder.bind(checkoutEventQueue).to(eventExchange).with("shipping.*"),
                BindingBuilder.bind(checkoutEventQueue).to(eventExchange).with("cart.*")
        );
    }

    @Bean
    public MessageConverter jacksonJsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
