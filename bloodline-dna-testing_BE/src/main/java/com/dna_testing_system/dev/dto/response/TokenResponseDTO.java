package com.dna_testing_system.dev.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for token refresh endpoint.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponseDTO {

    @JsonProperty("accessToken")
    private String accessToken;

    @JsonProperty("refreshToken")
    private String refreshToken;

    @JsonProperty("tokenType")
    private String tokenType;

    @JsonProperty("expiresIn")
    private Long expiresIn;
}
