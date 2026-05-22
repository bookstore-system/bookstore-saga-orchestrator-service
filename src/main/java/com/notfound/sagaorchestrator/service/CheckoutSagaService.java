package com.notfound.sagaorchestrator.service;

import com.notfound.sagaorchestrator.messaging.event.*;
import com.notfound.sagaorchestrator.model.dto.request.CheckoutRequest;
import com.notfound.sagaorchestrator.model.dto.response.CheckoutSagaResponse;

import java.util.UUID;

public interface CheckoutSagaService {
    CheckoutSagaResponse startCheckout(String userId, String authorization, CheckoutRequest request);
    CheckoutSagaResponse getSaga(UUID sagaId);
    void handleOrderCreated(OrderCreatedEvent event);
    void handleStockReserved(com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage event);
    void handlePromotionReserved(PromotionReservedEvent event);
    void handlePaymentCreated(PaymentCreatedEvent event);
    void handlePaymentCompleted(PaymentCompletedEvent event);
    void handleShippingCreated(ShippingCreatedEvent event);
    void handleOrderConfirmed(com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage event);
    void handleStockConfirmed(com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage event);
    void handlePromotionConfirmed(com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage event);
    void handleCartCleared(com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage event);
    void handleFailure(SagaFailureEvent event);
    void handleShippingCancelled(com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage event);
    void handlePaymentRefunded(com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage event);
    void handlePromotionReleased(com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage event);
    void handleStockReleased(com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage event);
    void handleOrderCancelled(com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage event);
    void expirePendingPayments();
}
