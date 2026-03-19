package com.dna_testing_system.dev.dto.request.staff;

import jakarta.validation.constraints.NotBlank;

public record PaymentStatusUpdateRequest(
        @NotBlank(message = "Payment status must not be blank")
        String paymentStatus
) {}
