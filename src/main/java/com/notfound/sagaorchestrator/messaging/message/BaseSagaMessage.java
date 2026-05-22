package com.notfound.sagaorchestrator.messaging.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseSagaMessage {
    private UUID eventId;
    private UUID sagaId;
    private UUID correlationId;
    private UUID causationId;
    private String type;
    private LocalDateTime occurredAt;
    private UUID orderId;
    private String userId;
    private Map<String, Object> payload;
}
