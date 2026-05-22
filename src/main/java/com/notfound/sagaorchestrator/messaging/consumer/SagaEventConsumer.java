package com.notfound.sagaorchestrator.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notfound.sagaorchestrator.config.RabbitMQConfig;
import com.notfound.sagaorchestrator.config.RoutingKeys;
import com.notfound.sagaorchestrator.exception.ResourceNotFoundException;
import com.notfound.sagaorchestrator.messaging.event.*;
import com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage;
import com.notfound.sagaorchestrator.service.CheckoutSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventConsumer {

    private final ObjectMapper objectMapper;
    private final CheckoutSagaService checkoutSagaService;

    @RabbitListener(
            queues = RabbitMQConfig.CHECKOUT_EVENT_QUEUE,
            containerFactory = "rawMessageListenerContainerFactory")
    public void handleEvent(Message message) throws Exception {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.info("Consume saga event routingKey={}", routingKey);

        try {
            switch (routingKey) {
                case RoutingKeys.ORDER_CREATED ->
                        checkoutSagaService.handleOrderCreated(read(message, OrderCreatedEvent.class));
                case RoutingKeys.BOOK_STOCK_RESERVED ->
                        checkoutSagaService.handleStockReserved(read(message, BaseSagaMessage.class));
                case RoutingKeys.PROMOTION_RESERVED ->
                        checkoutSagaService.handlePromotionReserved(read(message, PromotionReservedEvent.class));
                case RoutingKeys.PAYMENT_CREATED ->
                        checkoutSagaService.handlePaymentCreated(read(message, PaymentCreatedEvent.class));
                case RoutingKeys.PAYMENT_COMPLETED ->
                        checkoutSagaService.handlePaymentCompleted(read(message, PaymentCompletedEvent.class));
                case RoutingKeys.SHIPPING_CREATED ->
                        checkoutSagaService.handleShippingCreated(read(message, ShippingCreatedEvent.class));
                case RoutingKeys.ORDER_CONFIRMED ->
                        checkoutSagaService.handleOrderConfirmed(read(message, BaseSagaMessage.class));
                case RoutingKeys.BOOK_STOCK_CONFIRMED ->
                        checkoutSagaService.handleStockConfirmed(read(message, BaseSagaMessage.class));
                case RoutingKeys.PROMOTION_CONFIRMED ->
                        checkoutSagaService.handlePromotionConfirmed(read(message, BaseSagaMessage.class));
                case RoutingKeys.CART_CLEARED ->
                        checkoutSagaService.handleCartCleared(read(message, BaseSagaMessage.class));
                case RoutingKeys.ORDER_FAILED, RoutingKeys.BOOK_STOCK_FAILED, RoutingKeys.PROMOTION_FAILED,
                     RoutingKeys.PAYMENT_FAILED, RoutingKeys.SHIPPING_FAILED ->
                        checkoutSagaService.handleFailure(read(message, SagaFailureEvent.class));
                case RoutingKeys.SHIPPING_CANCELLED ->
                        checkoutSagaService.handleShippingCancelled(read(message, BaseSagaMessage.class));
                case RoutingKeys.PAYMENT_REFUNDED ->
                        checkoutSagaService.handlePaymentRefunded(read(message, BaseSagaMessage.class));
                case RoutingKeys.PROMOTION_RELEASED ->
                        checkoutSagaService.handlePromotionReleased(read(message, BaseSagaMessage.class));
                case RoutingKeys.BOOK_STOCK_RELEASED ->
                        checkoutSagaService.handleStockReleased(read(message, BaseSagaMessage.class));
                case RoutingKeys.ORDER_CANCELLED ->
                        checkoutSagaService.handleOrderCancelled(read(message, BaseSagaMessage.class));
                default -> log.warn("Ignore unsupported saga event routingKey={}", routingKey);
            }
        } catch (ResourceNotFoundException ex) {
            log.warn("Ignore saga event for missing saga routingKey={}: {}", routingKey, ex.getMessage());
        }
    }

    private <T> T read(Message message, Class<T> targetType) throws Exception {
        return objectMapper.readValue(message.getBody(), targetType);
    }
}
