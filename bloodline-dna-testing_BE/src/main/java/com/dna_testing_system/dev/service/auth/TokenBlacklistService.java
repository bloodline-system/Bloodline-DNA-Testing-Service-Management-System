package com.dna_testing_system.dev.service.auth;

public interface TokenBlacklistService {

    void blacklistAccessToken(String token);

    void blacklistRefreshToken(String refreshToken);

    boolean isAccessTokenBlacklisted(String token);

    boolean isRefreshTokenBlacklisted(String token);
}