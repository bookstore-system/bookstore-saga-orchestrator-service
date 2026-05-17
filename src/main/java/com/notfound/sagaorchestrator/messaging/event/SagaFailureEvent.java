package com.notfound.sagaorchestrator.messaging.event;

import com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SagaFailureEvent extends BaseSagaMessage {
    private String reason;
}

