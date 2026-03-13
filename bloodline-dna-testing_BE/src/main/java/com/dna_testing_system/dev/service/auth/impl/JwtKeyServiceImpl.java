package com.dna_testing_system.dev.service.auth.impl;

import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.properties.JwtProperties;
import com.dna_testing_system.dev.service.auth.JwtKeyService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@Transactional
class JwtKeyServiceImpl implements JwtKeyService {

    private final Key secretKey;

    public JwtKeyServiceImpl(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties
                .getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateToken(User user, Duration expiry) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("username", user.getUsername())
                .claim("roles", getRoles(user))
                .claim("permissions", getPermissions(user))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expiry)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String generateRefreshToken(User user, Duration expiry) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("token_type", "refresh")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expiry)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }
    @Override
    public List<String> extractRoles(String token) {
        List<?> roles = extractAllClaims(token).get("roles", List.class);
        if (roles == null) return Collections.emptyList();
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    @Override
    public List<String> extractPermissions(String token) {
        List<?> permissions = extractAllClaims(token).get("permissions", List.class);
        if (permissions == null) return Collections.emptyList();
        return permissions.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    private List<String> getRoles(User user) {
        return user.getUserRoles()
                .stream()
                .map(userRole -> userRole.getRole().getRoleName())
                .toList();
    }

    private List<String> getPermissions(User user) {
        return user.getUserRoles()
                .stream()
                .flatMap(userRole -> userRole.getRole().getRolePermissions().stream())
                .map(rolePermission -> rolePermission.getPermission().getPermissionName())
                .toList();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}