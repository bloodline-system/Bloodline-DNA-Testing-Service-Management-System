package com.dna_testing_system.dev.properties;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT configuration properties loaded from application.yaml.
 * Prefix: identity.jwt
 */
@ConfigurationProperties(prefix = "identity.jwt")
@Component
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtProperties {

    /** Secret key for signing JWT tokens */
    String secretKey;

    /** Access token expiration duration (default: 15 minutes) */
    Duration tokenExp;

    /** Refresh token expiration duration (default: 1 day) */
    Duration refreshTokenExp;
}
