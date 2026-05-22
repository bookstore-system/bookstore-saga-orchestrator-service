package com.notfound.sagaorchestrator.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {

    @NotBlank
    private String addressId;

    @NotBlank
    private String paymentMethod;

    private String note;
    private String discountCode;
    private String redirectUrl;
    private Double shippingFee;

    @NotEmpty
    private List<String> bookIds;
}
