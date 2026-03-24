package com.dna_testing_system.dev.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthTokensResponseDTO {
        @JsonProperty("accessToken")
        @JsonAlias("access_token")
        String accessToken;

        @JsonProperty("refreshToken")
        @JsonAlias("refresh_token")
        String refreshToken;

        @JsonProperty("expiresIn")
        @JsonAlias("expires_in")
        Long expiresIn;
        // Only user identifier — client can fetch full profile separately
        @JsonProperty("userId")
        @JsonAlias("user_id")
        String userId;
}