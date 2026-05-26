package com.notfound.sagaorchestrator.messaging.command;

import com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateShippingCommand extends BaseSagaMessage {
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private String shippingProvince;
    private String shippingDistrict;
    private String shippingWard;
    private String shippingNote;
    private Double codAmount;
    private Double expectedShippingFee;
}
