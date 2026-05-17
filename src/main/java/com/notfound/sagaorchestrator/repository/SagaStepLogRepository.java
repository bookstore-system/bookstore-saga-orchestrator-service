package com.notfound.sagaorchestrator.repository;

import com.notfound.sagaorchestrator.model.entity.SagaStepLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaStepLogRepository extends JpaRepository<SagaStepLog, Long> {
}

