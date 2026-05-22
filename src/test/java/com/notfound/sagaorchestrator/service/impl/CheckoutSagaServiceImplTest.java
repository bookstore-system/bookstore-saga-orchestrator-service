package com.notfound.sagaorchestrator.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notfound.sagaorchestrator.config.RoutingKeys;
import com.notfound.sagaorchestrator.messaging.command.CreateOrderCommand;
import com.notfound.sagaorchestrator.messaging.command.ReserveStockCommand;
import com.notfound.sagaorchestrator.messaging.event.OrderCreatedEvent;
import com.notfound.sagaorchestrator.messaging.event.PaymentCompletedEvent;
import com.notfound.sagaorchestrator.messaging.event.SagaFailureEvent;
import com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage;
import com.notfound.sagaorchestrator.messaging.message.StockItemPayload;
import com.notfound.sagaorchestrator.messaging.producer.SagaMessageProducer;
import com.notfound.sagaorchestrator.model.dto.request.CheckoutRequest;
import com.notfound.sagaorchestrator.model.entity.SagaInstance;
import com.notfound.sagaorchestrator.model.enums.CompensationStage;
import com.notfound.sagaorchestrator.model.enums.SagaStatus;
import com.notfound.sagaorchestrator.repository.ProcessedMessageRepository;
import com.notfound.sagaorchestrator.repository.SagaInstanceRepository;
import com.notfound.sagaorchestrator.repository.SagaStepLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutSagaServiceImplTest {

    @Mock private SagaInstanceRepository sagaInstanceRepository;
    @Mock private SagaStepLogRepository sagaStepLogRepository;
    @Mock private ProcessedMessageRepository processedMessageRepository;
    @Mock private SagaMessageProducer sagaMessageProducer;

    private CheckoutSagaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CheckoutSagaServiceImpl(
                sagaInstanceRepository,
                sagaStepLogRepository,
                processedMessageRepository,
                sagaMessageProducer,
                new ObjectMapper()
        );
    }

    @Test
    void startCheckout_createsSagaAndSendsOrderCreateCommand() {
        CheckoutRequest request = new CheckoutRequest();
        request.setAddressId("addr-1");
        request.setPaymentMethod("COD");
        request.setBookIds(List.of("book-1"));

        var response = service.startCheckout("user-1", "Bearer access-token", request);

        assertThat(response.getSagaId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(SagaStatus.STARTED);
        verify(sagaInstanceRepository).save(any(SagaInstance.class));
        ArgumentCaptor<CreateOrderCommand> commandCaptor = ArgumentCaptor.forClass(CreateOrderCommand.class);
        verify(sagaMessageProducer).sendCommand(eq(RoutingKeys.ORDER_CREATE_COMMAND), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getAuthorization()).isEqualTo("Bearer access-token");
    }

    @Test
    void startCheckout_withoutAuthorization_throwsBusinessException() {
        CheckoutRequest request = new CheckoutRequest();
        request.setAddressId("addr-1");
        request.setPaymentMethod("COD");
        request.setBookIds(List.of("book-1"));

        assertThatThrownBy(() -> service.startCheckout("user-1", " ", request))
                .isInstanceOf(com.notfound.sagaorchestrator.exception.BusinessException.class)
                .hasMessageContaining("authorization");
        verifyNoInteractions(sagaMessageProducer);
    }

    @Test
    void handleOrderCreated_sendsReserveStockCommand() {
        UUID sagaId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        SagaInstance saga = SagaInstance.builder()
                .sagaId(sagaId)
                .userId("user-1")
                .paymentMethod("COD")
                .addressId("addr-1")
                .status(SagaStatus.STARTED)
                .compensationStage(CompensationStage.NONE)
                .build();
        when(processedMessageRepository.existsById(eventId)).thenReturn(false);
        when(sagaInstanceRepository.findById(sagaId)).thenReturn(Optional.of(saga));

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .sagaId(sagaId)
                .type(RoutingKeys.ORDER_CREATED)
                .orderId(UUID.randomUUID())
                .totalAmount(120000D)
                .items(List.of(StockItemPayload.builder().bookId("book-1").quantity(1).build()))
                .build();

        service.handleOrderCreated(event);

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.ORDER_CREATED);
        ArgumentCaptor<ReserveStockCommand> commandCaptor = ArgumentCaptor.forClass(ReserveStockCommand.class);
        verify(sagaMessageProducer).sendCommand(eq(RoutingKeys.BOOK_STOCK_RESERVE_COMMAND), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getPayload()).containsKey("items");
    }

    @Test
    void handlePaymentCompleted_sendsShippingCommand() {
        UUID sagaId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        SagaInstance saga = SagaInstance.builder()
                .sagaId(sagaId)
                .userId("user-1")
                .paymentMethod("VNPAY")
                .addressId("addr-1")
                .status(SagaStatus.PAYMENT_PENDING)
                .orderId(UUID.randomUUID())
                .totalAmount(120000D)
                .compensationStage(CompensationStage.NONE)
                .build();
        when(processedMessageRepository.existsById(eventId)).thenReturn(false);
        when(sagaInstanceRepository.findById(sagaId)).thenReturn(Optional.of(saga));

        service.handlePaymentCompleted(PaymentCompletedEvent.builder()
                .eventId(eventId)
                .sagaId(sagaId)
                .type(RoutingKeys.PAYMENT_COMPLETED)
                .paymentId(UUID.randomUUID())
                .build());

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.PAYMENT_COMPLETED);
        verify(sagaMessageProducer).sendCommand(eq(RoutingKeys.SHIPPING_CREATE_COMMAND), any());
    }

    @Test
    void handleFailure_fromStockReserved_releasesStockFirst() {
        UUID sagaId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        SagaInstance saga = SagaInstance.builder()
                .sagaId(sagaId)
                .userId("user-1")
                .paymentMethod("COD")
                .addressId("addr-1")
                .orderId(UUID.randomUUID())
                .status(SagaStatus.STOCK_RESERVED)
                .compensationStage(CompensationStage.NONE)
                .build();
        when(processedMessageRepository.existsById(eventId)).thenReturn(false);
        when(sagaInstanceRepository.findById(sagaId)).thenReturn(Optional.of(saga));

        service.handleFailure(SagaFailureEvent.builder()
                .eventId(eventId)
                .sagaId(sagaId)
                .type(RoutingKeys.PROMOTION_FAILED)
                .reason("promotion invalid")
                .build());

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
        assertThat(saga.getCompensationStage()).isEqualTo(CompensationStage.RELEASE_STOCK);
        verify(sagaMessageProducer).sendCommand(eq(RoutingKeys.BOOK_STOCK_RELEASE_COMMAND), any());
    }

    @Test
    void duplicateEvent_isIgnored() {
        UUID eventId = UUID.randomUUID();
        when(processedMessageRepository.existsById(eventId)).thenReturn(true);

        service.handleStockConfirmed(BaseSagaMessage.builder()
                .eventId(eventId)
                .sagaId(UUID.randomUUID())
                .type(RoutingKeys.BOOK_STOCK_CONFIRMED)
                .build());

        verifyNoInteractions(sagaInstanceRepository);
    }

    @Test
    void handleFailure_afterCompletion_isIgnored() {
        UUID sagaId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        SagaInstance saga = SagaInstance.builder()
                .sagaId(sagaId)
                .userId("user-1")
                .paymentMethod("COD")
                .addressId("addr-1")
                .orderId(UUID.randomUUID())
                .status(SagaStatus.COMPLETED)
                .compensationStage(CompensationStage.NONE)
                .build();
        when(processedMessageRepository.existsById(eventId)).thenReturn(false);
        when(sagaInstanceRepository.findById(sagaId)).thenReturn(Optional.of(saga));

        service.handleFailure(SagaFailureEvent.builder()
                .eventId(eventId)
                .sagaId(sagaId)
                .type(RoutingKeys.SHIPPING_FAILED)
                .reason("late failure")
                .build());

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        verify(sagaMessageProducer, never()).sendCommand(anyString(), any());
    }

    @Test
    void handleFailure_fromStarted_failsWithoutCompensation() {
        UUID sagaId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        SagaInstance saga = SagaInstance.builder()
                .sagaId(sagaId)
                .userId("user-1")
                .paymentMethod("COD")
                .addressId("addr-1")
                .status(SagaStatus.STARTED)
                .compensationStage(CompensationStage.NONE)
                .build();
        when(processedMessageRepository.existsById(eventId)).thenReturn(false);
        when(sagaInstanceRepository.findById(sagaId)).thenReturn(Optional.of(saga));

        service.handleFailure(SagaFailureEvent.builder()
                .eventId(eventId)
                .sagaId(sagaId)
                .type(RoutingKeys.ORDER_FAILED)
                .reason("order create failed")
                .build());

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.FAILED);
        assertThat(saga.getCompensationStage()).isEqualTo(CompensationStage.DONE);
        verify(sagaMessageProducer, never()).sendCommand(anyString(), any());
        verify(sagaMessageProducer).publishEvent(eq(RoutingKeys.CHECKOUT_FAILED), any());
    }
}
