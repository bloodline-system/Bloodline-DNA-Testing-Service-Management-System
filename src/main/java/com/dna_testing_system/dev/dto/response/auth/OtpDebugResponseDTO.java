package com.dna_testing_system.dev.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
public record OtpDebugResponseDTO(
        @JsonProperty("otp_code")
        String otpCode
) {
}