package com.dna_testing_system.dev.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record RegisterResponseDTO (
        @JsonProperty("sign-up-id")
        String signUpId
) {
}