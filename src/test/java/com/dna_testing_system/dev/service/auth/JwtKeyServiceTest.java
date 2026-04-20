package com.dna_testing_system.dev.service.auth;

import com.dna_testing_system.dev.entity.Permission;
import com.dna_testing_system.dev.entity.Role;
import com.dna_testing_system.dev.entity.RolePermission;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserRole;
import com.dna_testing_system.dev.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtKeyServiceTest {

    private static final String SECRET_KEY = "01234567890123456789012345678901";
    private JwtKeyService jwtKeyService;

    @BeforeEach
    void setUp() {
        jwtKeyService = createService();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void generateTokenContainsUsernameRolesAndPermissions() {
        User user = buildUserWithRoleAndPermission("alice", "ROLE_USER", "READ_ORDER");

        String token = jwtKeyService.generateToken(user, Duration.ofMinutes(5));

        assertTrue(jwtKeyService.validateToken(token));
        assertEquals("alice", jwtKeyService.extractUsername(token));
        assertEquals(List.of("ROLE_USER"), jwtKeyService.extractRoles(token));
        assertEquals(List.of("READ_ORDER"), jwtKeyService.extractPermissions(token));
    }

    @Test
    void generateRefreshTokenIsValidAndHasNoRoleOrPermissionClaims() {
        User user = User.builder().username("bob").userRoles(Set.of()).build();

        String refreshToken = jwtKeyService.generateRefreshToken(user, Duration.ofMinutes(5));

        assertTrue(jwtKeyService.validateToken(refreshToken));
        assertEquals("bob", jwtKeyService.extractUsername(refreshToken));
        assertTrue(jwtKeyService.extractRoles(refreshToken).isEmpty());
        assertTrue(jwtKeyService.extractPermissions(refreshToken).isEmpty());
    }

    @Test
    void validateTokenReturnsFalseForMalformedToken() {
        assertFalse(jwtKeyService.validateToken("not-a-jwt-token"));
    }

    @Test
    void extractRolesFiltersOutNonStringValues() {
        String token = Jwts.builder()
                .setSubject("alice")
                .claim("roles", List.of("ROLE_ADMIN", 10, true))
                .setExpiration(Date.from(Instant.now().plusSeconds(120)))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertEquals(List.of("ROLE_ADMIN"), jwtKeyService.extractRoles(token));
    }

    @Test
    void extractPermissionsFiltersOutNonStringValues() {
        String token = Jwts.builder()
                .setSubject("alice")
                .claim("permissions", List.of("WRITE_ORDER", 25, false))
                .setExpiration(Date.from(Instant.now().plusSeconds(120)))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertEquals(List.of("WRITE_ORDER"), jwtKeyService.extractPermissions(token));
    }

    @Test
    void extractUsernameReturnsTokenSubject() {
        String token = Jwts.builder()
                .setSubject("charlie")
                .setExpiration(Date.from(Instant.now().plusSeconds(120)))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        assertEquals("charlie", jwtKeyService.extractUsername(token));
    }

    private JwtKeyService createService() {
        try {
            JwtProperties properties = new JwtProperties();
            properties.setSecretKey(SECRET_KEY);

            Class<?> implClass = Class.forName("com.dna_testing_system.dev.service.auth.impl.JwtKeyServiceImpl");
            Constructor<?> constructor = implClass.getDeclaredConstructor(JwtProperties.class);
            constructor.setAccessible(true);
            return (JwtKeyService) constructor.newInstance(properties);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create JwtKeyServiceImpl for tests", e);
        }
    }

    private User buildUserWithRoleAndPermission(String username, String roleName, String permissionName) {
        Permission permission = Permission.builder()
                .permissionName(permissionName)
                .build();
        Role role = Role.builder()
                .roleName(roleName)
                .build();
        RolePermission rolePermission = RolePermission.builder()
                .role(role)
                .permission(permission)
                .build();
        role.setRolePermissions(Set.of(rolePermission));

        UserRole userRole = UserRole.builder()
                .role(role)
                .build();

        return User.builder()
                .username(username)
                .userRoles(Set.of(userRole))
                .build();
    }
}