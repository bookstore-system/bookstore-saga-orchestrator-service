package com.notfound.sagaorchestrator.repository;

import com.notfound.sagaorchestrator.model.entity.SagaInstance;
import com.notfound.sagaorchestrator.model.enums.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SagaInstanceRepository extends JpaRepository<SagaInstance, UUID> {
    List<SagaInstance> findByStatusAndUpdatedAtBefore(SagaStatus status, LocalDateTime cutoff);
}

