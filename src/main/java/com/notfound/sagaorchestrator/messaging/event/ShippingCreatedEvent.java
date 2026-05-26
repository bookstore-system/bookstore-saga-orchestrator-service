package com.notfound.sagaorchestrator.messaging.event;

import com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ShippingCreatedEvent extends BaseSagaMessage {
    private String shippingOrderCode;
    private Double totalFee;
    private String expectedDeliveryTime;
    private Integer codAmount;
}
