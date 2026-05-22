package com.notfound.sagaorchestrator.model.entity;

import com.notfound.sagaorchestrator.model.enums.CompensationStage;
import com.notfound.sagaorchestrator.model.enums.SagaStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_instance")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaInstance {

    @Id
    @Column(name = "saga_id", nullable = false, updatable = false)
    private UUID sagaId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    @Column(name = "address_id", nullable = false)
    private String addressId;

    @Column(name = "discount_code")
    private String discountCode;

    @Column(name = "discount_amount")
    private Double discountAmount;

    @Column(name = "redirect_url")
    private String redirectUrl;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "shipping_fee")
    private Double shippingFee;

    @Column(name = "payment_url", length = 1024)
    private String paymentUrl;

    @Column(name = "shipping_order_code")
    private String shippingOrderCode;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "recipient_phone")
    private String recipientPhone;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "shipping_province")
    private String shippingProvince;

    @Column(name = "shipping_district")
    private String shippingDistrict;

    @Column(name = "shipping_ward")
    private String shippingWard;

    @Column(name = "shipping_note")
    private String shippingNote;

    @Lob
    @Column(name = "payload_json")
    private String payloadJson;

    @Column(name = "last_error", length = 1024)
    private String lastError;

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_stage", nullable = false)
    private CompensationStage compensationStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_from_status")
    private SagaStatus compensationFromStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
