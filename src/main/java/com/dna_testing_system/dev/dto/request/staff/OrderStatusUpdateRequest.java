package com.dna_testing_system.dev.dto.request.staff;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusUpdateRequest(
        @NotBlank(message = "Status must not be blank")
        String status
) {}
