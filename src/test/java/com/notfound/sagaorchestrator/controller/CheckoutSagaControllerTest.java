package com.notfound.sagaorchestrator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notfound.sagaorchestrator.exception.GlobalExceptionHandler;
import com.notfound.sagaorchestrator.model.dto.request.CheckoutRequest;
import com.notfound.sagaorchestrator.model.dto.response.CheckoutSagaResponse;
import com.notfound.sagaorchestrator.model.enums.SagaStatus;
import com.notfound.sagaorchestrator.service.CheckoutSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckoutSagaControllerTest {

    @Mock
    private CheckoutSagaService checkoutSagaService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new CheckoutSagaController(checkoutSagaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void startCheckout_returnsAccepted() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setAddressId("addr-1");
        request.setPaymentMethod("COD");
        request.setBookIds(List.of("book-1"));

        UUID sagaId = UUID.randomUUID();
        when(checkoutSagaService.startCheckout(eq("user-1"), any()))
                .thenReturn(CheckoutSagaResponse.builder().sagaId(sagaId).status(SagaStatus.STARTED).build());

        mockMvc.perform(post("/api/v1/checkout")
                        .header("X-User-Id", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.result.sagaId").value(sagaId.toString()))
                .andExpect(jsonPath("$.result.status").value("STARTED"));
    }

    @Test
    void startCheckout_withoutUser_returnsUnauthorized() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        request.setAddressId("addr-1");
        request.setPaymentMethod("COD");
        request.setBookIds(List.of("book-1"));

        mockMvc.perform(post("/api/v1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

