package com.notfound.sagaorchestrator.messaging.command;

import com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReservePromotionCommand extends BaseSagaMessage {
    private String promotionCode;
    private Double orderTotalBeforeDiscount;
}

