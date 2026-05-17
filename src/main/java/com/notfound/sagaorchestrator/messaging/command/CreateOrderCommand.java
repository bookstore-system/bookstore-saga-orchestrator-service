package com.notfound.sagaorchestrator.messaging.command;

import com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateOrderCommand extends BaseSagaMessage {
    private String addressId;
    private String paymentMethod;
    private String note;
    private String discountCode;
    private String redirectUrl;
    private List<String> bookIds;
}

