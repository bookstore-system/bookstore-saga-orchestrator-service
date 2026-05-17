package com.notfound.sagaorchestrator.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_step_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaStepLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false)
    private UUID sagaId;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(nullable = false)
    private String status;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(length = 1024)
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

