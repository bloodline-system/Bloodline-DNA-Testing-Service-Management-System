package com.dna_testing_system.dev.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationOptRequestDTO(

        @NotBlank(message = "signUpId must not be blank")
        String signUpId,

        @NotBlank(message = "OTP must not be blank")
        @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits")
        String otp

) {}
