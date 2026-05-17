package com.notfound.sagaorchestrator.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notfound.sagaorchestrator.config.RoutingKeys;
import com.notfound.sagaorchestrator.exception.BusinessException;
import com.notfound.sagaorchestrator.exception.ResourceNotFoundException;
import com.notfound.sagaorchestrator.messaging.command.*;
import com.notfound.sagaorchestrator.messaging.event.*;
import com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage;
import com.notfound.sagaorchestrator.messaging.producer.SagaMessageProducer;
import com.notfound.sagaorchestrator.model.dto.request.CheckoutRequest;
import com.notfound.sagaorchestrator.model.dto.response.CheckoutSagaResponse;
import com.notfound.sagaorchestrator.model.entity.ProcessedMessage;
import com.notfound.sagaorchestrator.model.entity.SagaInstance;
import com.notfound.sagaorchestrator.model.entity.SagaStepLog;
import com.notfound.sagaorchestrator.model.enums.CompensationStage;
import com.notfound.sagaorchestrator.model.enums.SagaStatus;
import com.notfound.sagaorchestrator.repository.ProcessedMessageRepository;
import com.notfound.sagaorchestrator.repository.SagaInstanceRepository;
import com.notfound.sagaorchestrator.repository.SagaStepLogRepository;
import com.notfound.sagaorchestrator.service.CheckoutSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutSagaServiceImpl implements CheckoutSagaService {

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepLogRepository sagaStepLogRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final SagaMessageProducer sagaMessageProducer;
    private final ObjectMapper objectMapper;

    @Value("${saga.payment-timeout-minutes:15}")
    private long paymentTimeoutMinutes;

    @Override
    @Transactional
    public CheckoutSagaResponse startCheckout(String userId, CheckoutRequest request) {
        validatePaymentMethod(request.getPaymentMethod());

        UUID sagaId = UUID.randomUUID();
        SagaInstance saga = SagaInstance.builder()
                .sagaId(sagaId)
                .userId(userId)
                .paymentMethod(normalizePaymentMethod(request.getPaymentMethod()))
                .status(SagaStatus.STARTED)
                .addressId(request.getAddressId())
                .discountCode(blankToNull(request.getDiscountCode()))
                .redirectUrl(blankToNull(request.getRedirectUrl()))
                .payloadJson(writePayload(request))
                .compensationStage(CompensationStage.NONE)
                .build();
        sagaInstanceRepository.save(saga);

        CreateOrderCommand command = CreateOrderCommand.builder()
                .eventId(UUID.randomUUID())
                .sagaId(sagaId)
                .correlationId(sagaId)
                .type(RoutingKeys.ORDER_CREATE_COMMAND)
                .occurredAt(LocalDateTime.now())
                .userId(userId)
                .addressId(request.getAddressId())
                .paymentMethod(saga.getPaymentMethod())
                .note(request.getNote())
                .discountCode(saga.getDiscountCode())
                .redirectUrl(saga.getRedirectUrl())
                .bookIds(request.getBookIds())
                .build();
        sagaMessageProducer.sendCommand(RoutingKeys.ORDER_CREATE_COMMAND, command);
        logStep(sagaId, "ORDER_CREATE_COMMAND", "SENT", command.getEventId(), null);
        return mapToResponse(saga);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutSagaResponse getSaga(UUID sagaId) {
        return mapToResponse(findSaga(sagaId));
    }

    @Override
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (!markProcessed(event)) return;
        SagaInstance saga = findSaga(event.getSagaId());
        if (saga.getStatus() != SagaStatus.STARTED) return;

        saga.setOrderId(event.getOrderId());
        saga.setTotalAmount(event.getTotalAmount());
        saga.setRecipientName(event.getRecipientName());
        saga.setRecipientPhone(event.getRecipientPhone());
        saga.setShippingAddress(event.getShippingAddress());
        saga.setShippingProvince(event.getShippingProvince());
        saga.setShippingDistrict(event.getShippingDistrict());
        saga.setShippingWard(event.getShippingWard());
        saga.setShippingNote(event.getShippingNote());
        saga.setStatus(SagaStatus.ORDER_CREATED);
        sagaInstanceRepository.save(saga);
        logStep(saga.getSagaId(), "ORDER_CREATED", "DONE", event.getEventId(), null);

        ReserveStockCommand command = ReserveStockCommand.builder()
                .eventId(UUID.randomUUID())
                .sagaId(saga.getSagaId())
                .correlationId(saga.getSagaId())
                .causationId(event.getEventId())
                .type(RoutingKeys.BOOK_STOCK_RESERVE_COMMAND)
                .occurredAt(LocalDateTime.now())
                .orderId(saga.getOrderId())
                .userId(saga.getUserId())
                .items(event.getItems())
                .build();
        sagaMessageProducer.sendCommand(RoutingKeys.BOOK_STOCK_RESERVE_COMMAND, command);
        logStep(saga.getSagaId(), "BOOK_STOCK_RESERVE_COMMAND", "SENT", command.getEventId(), null);
    }

    @Override
    @Transactional
    public void handleStockReserved(BaseSagaMessage event) {
        if (!markProcessed(event)) return;
        SagaInstance saga = findSaga(event.getSagaId());
        if (saga.getStatus() != SagaStatus.ORDER_CREATED) return;

        saga.setStatus(SagaStatus.STOCK_RESERVED);
        sagaInstanceRepository.save(saga);
        logStep(saga.getSagaId(), "BOOK_STOCK_RESERVED", "DONE", event.getEventId(), null);

        if (hasPromotion(saga)) {
            ReservePromotionCommand command = ReservePromotionCommand.builder()
                    .eventId(UUID.randomUUID())
                    .sagaId(saga.getSagaId())
                    .correlationId(saga.getSagaId())
                    .causationId(event.getEventId())
                    .type(RoutingKeys.PROMOTION_RESERVE_COMMAND)
                    .occurredAt(LocalDateTime.now())
                    .orderId(saga.getOrderId())
                    .userId(saga.getUserId())
                    .promotionCode(saga.getDiscountCode())
                    .orderTotalBeforeDiscount(saga.getTotalAmount())
                    .build();
            sagaMessageProducer.sendCommand(RoutingKeys.PROMOTION_RESERVE_COMMAND, command);
            logStep(saga.getSagaId(), "PROMOTION_RESERVE_COMMAND", "SENT", command.getEventId(), null);
        } else {
            continueAfterPromotion(saga, event.getEventId());
        }
    }

    @Override
    @Transactional
    public void handlePromotionReserved(PromotionReservedEvent event) {
        if (!markProcessed(event)) return;
        SagaInstance saga = findSaga(event.getSagaId());
        if (saga.getStatus() != SagaStatus.STOCK_RESERVED) return;

        saga.setStatus(SagaStatus.PROMOTION_RESERVED);
        if (event.getFinalTotal() != null) {
            saga.setTotalAmount(event.getFinalTotal());
        }
        sagaInstanceRepository.save(saga);
        logStep(saga.getSagaId(), "PROMOTION_RESERVED", "DONE", event.getEventId(), null);
        continueAfterPromotion(saga, event.getEventId());
    }

    @Override
    @Transactional
    public void handlePaymentCreated(PaymentCreatedEvent event) {
        if (!markProcessed(event)) return;
        SagaInstance saga = findSaga(event.getSagaId());
        if (saga.getStatus() != SagaStatus.PAYMENT_PENDING) return;

        saga.setPaymentId(event.getPaymentId());
        saga.setPaymentUrl(event.getPaymentUrl());
        sagaInstanceRepository.save(saga);
        logStep(saga.getSagaId(), "PAYMENT_CREATED", "DONE", event.getEventId(), null);
    }

    @Override
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (!markProcessed(event)) return;
        SagaInstance saga = findSaga(event.getSagaId());
        if (saga.getStatus() != SagaStatus.PAYMENT_PENDING) return;

        saga.setPaymentId(event.getPaymentId());
        saga.setStatus(SagaStatus.PAYMENT_COMPLETED);
        sagaInstanceRepository.save(saga);
        logStep(saga.getSagaId(), "PAYMENT_COMPLETED", "DONE", event.getEventId(), null);
        sendCreateShippingCommand(saga, event.getEventId());
    }

    @Override
    @Transactional
    public void handleShippingCreated(ShippingCreatedEvent event) {
        if (!markProcessed(event)) return;
        SagaInstance saga = findSaga(event.getSagaId());
        if (saga.getStatus() != SagaStatus.PAYMENT_COMPLETED
                && saga.getStatus() != SagaStatus.PAYMENT_SKIPPED) {
            return;
        }

        saga.setShippingOrderCode(event.getShippingOrderCode());
        saga.setStatus(SagaStatus.SHIPPING_CREATED);
        sagaInstanceRepository.save(saga);
        logStep(saga.getSagaId(), "SHIPPING_CREATED", "DONE", event.getEventId(), null);
        sendSimpleCommand(saga, event.getEventId(), RoutingKeys.ORDER_CONFIRM_COMMAND, "ORDER_CONFIRM_COMMAND");
    }

    @Override
    @Transactional
    public void handleOrderConfirmed(BaseSagaMessage event) {
        if (!markProcessed(event)) return;
        SagaInstance saga = findSaga(event.getSagaId());
        if (saga.getStatus() != SagaStatus.SHIPPING_CREATED) return;

        saga.setStatus(SagaStatus.ORDER_CONFIRMED);
        sagaInstanceRepository.save(saga);
        logStep(saga.getSagaId(), "ORDER_CONFIRMED", "DONE", event.getEventId(), null);
        sendSimpleCommand(saga, event.getEventId(), RoutingKeys.BOOK_STOCK_CONFIRM_COMMAND, "BOOK_STOCK_CONFIRM_COMMAND");
    }

    @Override
    @Transactional
    public void handleStockConfirmed(BaseSagaMessage event) {
        if (!markProcessed(event)) return;
        SagaInstance saga = findSaga(event.getSagaId());
        if (saga.getStatus() != SagaStatus.ORDER_CONFIRMED) return;

        saga.setStatus(SagaStatus.STOCK_CONFIRMED);
        sagaInstanceRepository.save(saga);
        logStep(saga.getSagaId(), "BOOK_STOCK_CONFIRMED", "DONE", event.getEventId(), null);
        if (hasPromotion(saga)) {
            sendSimpleCommand(saga, event.getEventId(), RoutingKeys.PROMOTION_CONFIRM_COMMAND, "PROMOTION_CONFIRM_COMMAND");
        } else {
            sendSimpleCommand(saga, event.getEventId(), RoutingKeys.CART_CLEAR_COMMAND, "CART_CLEAR_COMMAND");
        }
    }

    @Override
    @Transactional
    public void handlePromotionConfirmed(BaseSagaMessage event) {
        if (!markProcessed(event)) return;
        SagaInstance saga = findSaga(event.getSagaId());
        if (saga.getStatus() != SagaStatus.STOCK_CONFIRMED) return;

        saga.setStatus(SagaStatus.PROMOTION_CONFIRMED);
        sagaInstanceRepository.save(saga);
        logStep(saga.getSagaId(), "PROMOTION_CONFIRMED", "DONE", event.getEventId(), null);
        sendSimpleCommand(saga, event.getEventId(), RoutingKeys.CART_CLEAR_COMMAND, "CART_CLEAR_COMMAND");
    }

    @Override
    @Transactional
    public void handleCartCleared(BaseSagaMessage event) {
        if (!markProcessed(event)) return;
        SagaInstance saga = findSaga(event.getSagaId());
        if (saga.getStatus() != SagaStatus.STOCK_CONFIRMED
                && saga.getStatus() != SagaStatus.PROMOTION_CONFIRMED) {
            return;
        }

        saga.setStatus(SagaStatus.CART_CLEARED);
        sagaInstanceRepository.save(saga);
        logStep(saga.getSagaId(), "CART_CLEARED", "DONE", event.getEventId(), null);

        saga.setStatus(SagaStatus.COMPLETED);
        sagaInstanceRepository.save(saga);
        sagaMessageProducer.publishEvent(RoutingKeys.CHECKOUT_COMPLETED, buildSimpleEvent(saga, RoutingKeys.CHECKOUT_COMPLETED, event.getEventId()));
        logStep(saga.getSagaId(), "CHECKOUT_COMPLETED", "DONE", event.getEventId(), null);
    }

    @Override
    @Transactional
    public void handleFailure(SagaFailureEvent event) {
        if (!markProcessed(event)) return;
        SagaInstance saga = findSaga(event.getSagaId());
        beginCompensation(saga, event.getReason(), event.getEventId());
    }

    @Override
    @Transactional
    public void handleShippingCancelled(BaseSagaMessage event) {
        advanceCompensation(event, CompensationStage.CANCEL_SHIPPING);
    }

    @Override
    @Transactional
    public void handlePaymentRefunded(BaseSagaMessage event) {
        advanceCompensation(event, CompensationStage.REFUND_PAYMENT);
    }

    @Override
    @Transactional
    public void handlePromotionReleased(BaseSagaMessage event) {
        advanceCompensation(event, CompensationStage.RELEASE_PROMOTION);
    }

    @Override
    @Transactional
    public void handleStockReleased(BaseSagaMessage event) {
        advanceCompensation(event, CompensationStage.RELEASE_STOCK);
    }

    @Override
    @Transactional
    public void handleOrderCancelled(BaseSagaMessage event) {
        advanceCompensation(event, CompensationStage.CANCEL_ORDER);
    }

    @Override
    @Scheduled(fixedDelayString = "${saga.timeout-scan-ms:60000}")
    @Transactional
    public void expirePendingPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(paymentTimeoutMinutes);
        sagaInstanceRepository.findByStatusAndUpdatedAtBefore(SagaStatus.PAYMENT_PENDING, cutoff)
                .forEach(saga -> {
                    beginCompensation(saga, "Payment timeout", UUID.randomUUID());
                });
    }

    private void continueAfterPromotion(SagaInstance saga, UUID causationId) {
        if (isOnlinePayment(saga)) {
            saga.setStatus(SagaStatus.PAYMENT_PENDING);
            sagaInstanceRepository.save(saga);
            CreatePaymentCommand command = CreatePaymentCommand.builder()
                    .eventId(UUID.randomUUID())
                    .sagaId(saga.getSagaId())
                    .correlationId(saga.getSagaId())
                    .causationId(causationId)
                    .type(RoutingKeys.PAYMENT_CREATE_COMMAND)
                    .occurredAt(LocalDateTime.now())
                    .orderId(saga.getOrderId())
                    .userId(saga.getUserId())
                    .amount(saga.getTotalAmount())
                    .paymentMethod(saga.getPaymentMethod())
                    .redirectUrl(saga.getRedirectUrl())
                    .build();
            sagaMessageProducer.sendCommand(RoutingKeys.PAYMENT_CREATE_COMMAND, command);
            logStep(saga.getSagaId(), "PAYMENT_CREATE_COMMAND", "SENT", command.getEventId(), null);
        } else {
            saga.setStatus(SagaStatus.PAYMENT_SKIPPED);
            sagaInstanceRepository.save(saga);
            sendCreateShippingCommand(saga, causationId);
        }
    }

    private void sendCreateShippingCommand(SagaInstance saga, UUID causationId) {
        CreateShippingCommand command = CreateShippingCommand.builder()
                .eventId(UUID.randomUUID())
                .sagaId(saga.getSagaId())
                .correlationId(saga.getSagaId())
                .causationId(causationId)
                .type(RoutingKeys.SHIPPING_CREATE_COMMAND)
                .occurredAt(LocalDateTime.now())
                .orderId(saga.getOrderId())
                .userId(saga.getUserId())
                .recipientName(saga.getRecipientName())
                .recipientPhone(saga.getRecipientPhone())
                .shippingAddress(saga.getShippingAddress())
                .shippingProvince(saga.getShippingProvince())
                .shippingDistrict(saga.getShippingDistrict())
                .shippingWard(saga.getShippingWard())
                .shippingNote(saga.getShippingNote())
                .codAmount(isOnlinePayment(saga) ? 0D : saga.getTotalAmount())
                .build();
        sagaMessageProducer.sendCommand(RoutingKeys.SHIPPING_CREATE_COMMAND, command);
        logStep(saga.getSagaId(), "SHIPPING_CREATE_COMMAND", "SENT", command.getEventId(), null);
    }

    private void beginCompensation(SagaInstance saga, String reason, UUID causationId) {
        if (saga.getStatus() == SagaStatus.COMPENSATING
                || saga.getStatus() == SagaStatus.FAILED
                || saga.getStatus() == SagaStatus.COMPLETED) {
            return;
        }
        SagaStatus previousStatus = saga.getStatus();
        if (previousStatus == SagaStatus.STARTED) {
            failWithoutCompensation(saga, reason, causationId);
            return;
        }
        saga.setCompensationFromStatus(previousStatus);
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setLastError(reason);
        saga.setCompensationStage(firstCompensationStage(saga, previousStatus));
        sagaInstanceRepository.save(saga);
        logStep(saga.getSagaId(), "COMPENSATION_STARTED", "SENT", causationId, reason);
        dispatchCompensationCommand(saga, causationId);
    }

    private void advanceCompensation(BaseSagaMessage event, CompensationStage completedStage) {
        if (!markProcessed(event)) return;
        SagaInstance saga = findSaga(event.getSagaId());
        if (saga.getStatus() != SagaStatus.COMPENSATING || saga.getCompensationStage() != completedStage) {
            return;
        }

        logStep(saga.getSagaId(), completedStage.name(), "DONE", event.getEventId(), null);
        saga.setCompensationStage(nextCompensationStage(saga, completedStage));
        if (saga.getCompensationStage() == CompensationStage.DONE) {
            saga.setStatus(SagaStatus.FAILED);
            sagaInstanceRepository.save(saga);
            sagaMessageProducer.publishEvent(RoutingKeys.CHECKOUT_FAILED, buildSimpleEvent(saga, RoutingKeys.CHECKOUT_FAILED, event.getEventId()));
            logStep(saga.getSagaId(), "CHECKOUT_FAILED", "DONE", event.getEventId(), saga.getLastError());
            return;
        }
        sagaInstanceRepository.save(saga);
        dispatchCompensationCommand(saga, event.getEventId());
    }

    private CompensationStage firstCompensationStage(SagaInstance saga, SagaStatus previousStatus) {
        if (previousStatus.ordinal() >= SagaStatus.SHIPPING_CREATED.ordinal() && saga.getShippingOrderCode() != null) {
            return CompensationStage.CANCEL_SHIPPING;
        }
        if (previousStatus.ordinal() >= SagaStatus.PAYMENT_COMPLETED.ordinal() && saga.getPaymentId() != null) {
            return CompensationStage.REFUND_PAYMENT;
        }
        if (previousStatus.ordinal() >= SagaStatus.PROMOTION_RESERVED.ordinal() && hasPromotion(saga)) {
            return CompensationStage.RELEASE_PROMOTION;
        }
        if (previousStatus.ordinal() >= SagaStatus.STOCK_RESERVED.ordinal()) {
            return CompensationStage.RELEASE_STOCK;
        }
        return CompensationStage.CANCEL_ORDER;
    }

    private CompensationStage nextCompensationStage(SagaInstance saga, CompensationStage completedStage) {
        return switch (completedStage) {
            case CANCEL_SHIPPING -> saga.getPaymentId() != null
                    ? CompensationStage.REFUND_PAYMENT
                    : nextAfterPaymentRefund(saga);
            case REFUND_PAYMENT -> nextAfterPaymentRefund(saga);
            case RELEASE_PROMOTION -> CompensationStage.RELEASE_STOCK;
            case RELEASE_STOCK -> CompensationStage.CANCEL_ORDER;
            case CANCEL_ORDER -> CompensationStage.DONE;
            default -> CompensationStage.DONE;
        };
    }

    private CompensationStage nextAfterPaymentRefund(SagaInstance saga) {
        return hasPromotion(saga) ? CompensationStage.RELEASE_PROMOTION : CompensationStage.RELEASE_STOCK;
    }

    private void dispatchCompensationCommand(SagaInstance saga, UUID causationId) {
        String routingKey = switch (saga.getCompensationStage()) {
            case CANCEL_SHIPPING -> RoutingKeys.SHIPPING_CANCEL_COMMAND;
            case REFUND_PAYMENT -> RoutingKeys.PAYMENT_REFUND_COMMAND;
            case RELEASE_PROMOTION -> RoutingKeys.PROMOTION_RELEASE_COMMAND;
            case RELEASE_STOCK -> RoutingKeys.BOOK_STOCK_RELEASE_COMMAND;
            case CANCEL_ORDER -> RoutingKeys.ORDER_CANCEL_COMMAND;
            default -> null;
        };
        if (routingKey == null) {
            return;
        }
        sendSimpleCommand(saga, causationId, routingKey, saga.getCompensationStage().name());
    }

    private void failWithoutCompensation(SagaInstance saga, String reason, UUID causationId) {
        saga.setStatus(SagaStatus.FAILED);
        saga.setLastError(reason);
        saga.setCompensationFromStatus(SagaStatus.STARTED);
        saga.setCompensationStage(CompensationStage.DONE);
        sagaInstanceRepository.save(saga);
        sagaMessageProducer.publishEvent(
                RoutingKeys.CHECKOUT_FAILED,
                buildSimpleEvent(saga, RoutingKeys.CHECKOUT_FAILED, causationId)
        );
        logStep(saga.getSagaId(), "CHECKOUT_FAILED", "DONE", causationId, reason);
    }

    private void sendSimpleCommand(SagaInstance saga, UUID causationId, String routingKey, String stepName) {
        SimpleSagaCommand command = SimpleSagaCommand.builder()
                .eventId(UUID.randomUUID())
                .sagaId(saga.getSagaId())
                .correlationId(saga.getSagaId())
                .causationId(causationId)
                .type(routingKey)
                .occurredAt(LocalDateTime.now())
                .orderId(saga.getOrderId())
                .userId(saga.getUserId())
                .build();
        sagaMessageProducer.sendCommand(routingKey, command);
        logStep(saga.getSagaId(), stepName, "SENT", command.getEventId(), null);
    }

    private BaseSagaMessage buildSimpleEvent(SagaInstance saga, String type, UUID causationId) {
        return BaseSagaMessage.builder()
                .eventId(UUID.randomUUID())
                .sagaId(saga.getSagaId())
                .correlationId(saga.getSagaId())
                .causationId(causationId)
                .type(type)
                .occurredAt(LocalDateTime.now())
                .orderId(saga.getOrderId())
                .userId(saga.getUserId())
                .build();
    }

    private boolean markProcessed(BaseSagaMessage event) {
        if (event.getEventId() == null) {
            throw new BusinessException("eventId is required");
        }
        if (processedMessageRepository.existsById(event.getEventId())) {
            log.info("Skip duplicate event eventId={} type={}", event.getEventId(), event.getType());
            return false;
        }
        processedMessageRepository.save(ProcessedMessage.builder()
                .eventId(event.getEventId())
                .sagaId(event.getSagaId())
                .messageType(event.getType())
                .processedAt(LocalDateTime.now())
                .build());
        return true;
    }

    private SagaInstance findSaga(UUID sagaId) {
        return sagaInstanceRepository.findById(sagaId)
                .orElseThrow(() -> new ResourceNotFoundException("Saga", sagaId.toString()));
    }

    private CheckoutSagaResponse mapToResponse(SagaInstance saga) {
        return CheckoutSagaResponse.builder()
                .sagaId(saga.getSagaId())
                .orderId(saga.getOrderId())
                .status(saga.getStatus())
                .totalAmount(saga.getTotalAmount())
                .paymentUrl(saga.getPaymentUrl())
                .lastError(saga.getLastError())
                .createdAt(saga.getCreatedAt())
                .updatedAt(saga.getUpdatedAt())
                .build();
    }

    private void logStep(UUID sagaId, String stepName, String status, UUID messageId, String details) {
        sagaStepLogRepository.save(SagaStepLog.builder()
                .sagaId(sagaId)
                .stepName(stepName)
                .status(status)
                .messageId(messageId)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void validatePaymentMethod(String paymentMethod) {
        String normalized = normalizePaymentMethod(paymentMethod);
        if (!normalized.equals("COD")
                && !normalized.equals("VNPAY")
                && !normalized.equals("MOMO")
                && !normalized.equals("ZALOPAY")) {
            throw new BusinessException("Unsupported payment method: " + paymentMethod);
        }
    }

    private boolean isOnlinePayment(SagaInstance saga) {
        return !"COD".equals(saga.getPaymentMethod());
    }

    private boolean hasPromotion(SagaInstance saga) {
        return saga.getDiscountCode() != null && !saga.getDiscountCode().isBlank();
    }

    private String normalizePaymentMethod(String paymentMethod) {
        return paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String writePayload(CheckoutRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Unable to serialize checkout request");
        }
    }
}
