package com.dna_testing_system.dev.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(@NotBlank String refreshToken) {
}