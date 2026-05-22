package com.notfound.sagaorchestrator.messaging.producer;

import com.notfound.sagaorchestrator.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendCommand(String routingKey, Object payload) {
        log.info("Publish command routingKey={}", routingKey);
        rabbitTemplate.convertAndSend(RabbitMQConfig.COMMAND_EXCHANGE, routingKey, payload, this::removeJavaTypeHeaders);
    }

    public void publishEvent(String routingKey, Object payload) {
        log.info("Publish event routingKey={}", routingKey);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EVENT_EXCHANGE, routingKey, payload, this::removeJavaTypeHeaders);
    }

    private Message removeJavaTypeHeaders(Message message) {
        message.getMessageProperties().getHeaders().remove("__TypeId__");
        message.getMessageProperties().getHeaders().remove("__ContentTypeId__");
        message.getMessageProperties().getHeaders().remove("__KeyTypeId__");
        return message;
    }
}
