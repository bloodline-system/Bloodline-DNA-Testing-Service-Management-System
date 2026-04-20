package com.dna_testing_system.dev.service.auth;

import com.dna_testing_system.dev.properties.JwtProperties;
import com.dna_testing_system.dev.service.auth.impl.TokenBlacklistServiceImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    private static final String SECRET_KEY = "01234567890123456789012345678901";

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtProperties jwtProperties;
    private TokenBlacklistServiceImpl service;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey(SECRET_KEY);
        jwtProperties.setTokenExp(Duration.ofMinutes(15));
        jwtProperties.setRefreshTokenExp(Duration.ofDays(1));

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new TokenBlacklistServiceImpl(redisTemplate, jwtProperties);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void blacklistAccessTokenStoresTokenWithPositiveTtl() {
        String token = tokenWithExpiration(Instant.now().plusSeconds(120));

        service.blacklistAccessToken(token);

        verify(valueOperations).set(eq("blacklist:access:" + token), eq(token),
                argThat((ArgumentMatcher<Duration>) ttl -> ttl != null
                        && ttl.compareTo(Duration.ZERO) > 0
                        && ttl.compareTo(Duration.ofMinutes(3)) < 0));
    }

    @Test
    void blacklistAccessTokenUsesFallbackTtlForInvalidToken() {
        service.blacklistAccessToken("invalid-token");

        verify(valueOperations).set("blacklist:access:invalid-token", "invalid-token", Duration.ofMinutes(15));
    }

    @Test
    void blacklistAccessTokenSkipsStoreWhenResolvedTtlIsZero() {
        jwtProperties.setTokenExp(Duration.ZERO);

        service.blacklistAccessToken("invalid-token");

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void blacklistRefreshTokenUsesFallbackWhenTokenHasNoExpirationClaim() {
        String tokenWithoutExp = tokenWithoutExpiration();

        service.blacklistRefreshToken(tokenWithoutExp);

        verify(valueOperations).set("blacklist:refresh:" + tokenWithoutExp, tokenWithoutExp, Duration.ofDays(1));
    }

    @Test
    void isAccessTokenBlacklistedReturnsTrueOnlyWhenRedisReturnsTrue() {
        when(redisTemplate.hasKey("blacklist:access:a")).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:access:b")).thenReturn(false);
        when(redisTemplate.hasKey("blacklist:access:c")).thenReturn(null);

        assertTrue(service.isAccessTokenBlacklisted("a"));
        assertFalse(service.isAccessTokenBlacklisted("b"));
        assertFalse(service.isAccessTokenBlacklisted("c"));
    }

    @Test
    void isRefreshTokenBlacklistedReturnsTrueOnlyWhenRedisReturnsTrue() {
        when(redisTemplate.hasKey("blacklist:refresh:a")).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:refresh:b")).thenReturn(false);
        when(redisTemplate.hasKey("blacklist:refresh:c")).thenReturn(null);

        assertTrue(service.isRefreshTokenBlacklisted("a"));
        assertFalse(service.isRefreshTokenBlacklisted("b"));
        assertFalse(service.isRefreshTokenBlacklisted("c"));
    }

    private String tokenWithExpiration(Instant expiration) {
        return Jwts.builder()
                .setSubject("alice")
                .setExpiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    private String tokenWithoutExpiration() {
        return Jwts.builder()
                .setSubject("alice")
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}