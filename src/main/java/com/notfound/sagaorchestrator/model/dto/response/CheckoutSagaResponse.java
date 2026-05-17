package com.notfound.sagaorchestrator.model.dto.response;

import com.notfound.sagaorchestrator.model.enums.SagaStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CheckoutSagaResponse {
    private UUID sagaId;
    private UUID orderId;
    private SagaStatus status;
    private Double totalAmount;
    private String paymentUrl;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

