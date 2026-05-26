package com.notfound.sagaorchestrator.messaging.event;

import com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PromotionReservedEvent extends BaseSagaMessage {
    private Double discountAmount;
    private Double finalTotal;
}
