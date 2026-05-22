package com.notfound.sagaorchestrator.messaging.command;

import com.notfound.sagaorchestrator.messaging.message.BaseSagaMessage;
import com.notfound.sagaorchestrator.messaging.message.StockItemPayload;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReserveStockCommand extends BaseSagaMessage {
    private List<StockItemPayload> items;
}
