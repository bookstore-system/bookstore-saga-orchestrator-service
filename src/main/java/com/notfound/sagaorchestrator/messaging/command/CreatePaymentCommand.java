package com.notfound.sagaorchestrator.messaging.command;

import com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreatePaymentCommand extends BaseSagaMessage {
    private Double amount;
    private String paymentMethod;
    private String redirectUrl;
}

