package com.notfound.sagaorchestrator.messaging.command;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfirmOrderCommand extends SimpleSagaCommand {
    private Double totalAmount;
    private Double discountAmount;
    private Double shippingFee;
    private UUID paymentId;
    private String shippingOrderCode;
}
