package com.notfound.sagaorchestrator.messaging.event;

import com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentCreatedEvent extends BaseSagaMessage {
    private UUID paymentId;
    private String paymentUrl;
}

