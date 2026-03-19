package com.dna_testing_system.dev.service.auth.impl;

import com.dna_testing_system.dev.exception.InvalidTokenException;
import com.dna_testing_system.dev.properties.JwtProperties;
import com.dna_testing_system.dev.service.auth.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final String ACCESS_PREFIX = "blacklist:access:";
    private static final String REFRESH_PREFIX = "blacklist:refresh:";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;

    @Override
    public void blacklistAccessToken(String token) {
        putWithTtl(ACCESS_PREFIX + token, token, resolveTtlFromToken(token, jwtProperties.getTokenExp()));
    }

    @Override
    public void blacklistRefreshToken(String refreshToken) {
        putWithTtl(REFRESH_PREFIX + refreshToken, refreshToken,
                resolveTtlFromToken(refreshToken, jwtProperties.getRefreshTokenExp()));
    }

    @Override
    public boolean isAccessTokenBlacklisted(String token) {
        Boolean exists = redisTemplate.hasKey(ACCESS_PREFIX + token);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public boolean isRefreshTokenBlacklisted(String token) {
        Boolean exists = redisTemplate.hasKey(REFRESH_PREFIX + token);
        return Boolean.TRUE.equals(exists);
    }

    private void putWithTtl(String key, String value, Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    private Duration resolveTtlFromToken(String token, Duration fallbackTtl) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            if (expiration == null) {
                return fallbackTtl;
            }
            Duration remaining = Duration.between(Instant.now(), expiration.toInstant());
            return remaining.isNegative() ? Duration.ZERO : remaining;
        } catch (Exception ignored) {
            return fallbackTtl;
        }
    }

    private Claims parseClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token is required");
        }
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}