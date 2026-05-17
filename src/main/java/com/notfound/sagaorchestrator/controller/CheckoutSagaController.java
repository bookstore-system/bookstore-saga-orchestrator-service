package com.notfound.sagaorchestrator.controller;

import com.notfound.sagaorchestrator.model.dto.request.CheckoutRequest;
import com.notfound.sagaorchestrator.model.dto.response.ApiResponse;
import com.notfound.sagaorchestrator.model.dto.response.CheckoutSagaResponse;
import com.notfound.sagaorchestrator.service.CheckoutSagaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutSagaController {

    private final CheckoutSagaService checkoutSagaService;

    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutSagaResponse>> startCheckout(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CheckoutRequest request
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.<CheckoutSagaResponse>builder()
                            .code(4001)
                            .message("Unauthenticated")
                            .build());
        }
        return ResponseEntity.accepted()
                .body(ApiResponse.<CheckoutSagaResponse>builder()
                        .code(1000)
                        .message("Checkout saga started")
                        .result(checkoutSagaService.startCheckout(userId, request))
                        .build());
    }

    @GetMapping("/{sagaId}")
    public ResponseEntity<ApiResponse<CheckoutSagaResponse>> getSaga(@PathVariable UUID sagaId) {
        return ResponseEntity.ok(ApiResponse.<CheckoutSagaResponse>builder()
                .code(1000)
                .message("Checkout saga loaded")
                .result(checkoutSagaService.getSaga(sagaId))
                .build());
    }
}

