package com.notfound.sagaorchestrator.repository;

import com.notfound.sagaorchestrator.model.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, UUID> {
}

