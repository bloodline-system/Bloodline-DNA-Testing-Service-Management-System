package com.dna_testing_system.dev.integration.auth;

import com.dna_testing_system.dev.dto.request.auth.AuthenticationRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.RegisterRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.RefreshTokenRequestDTO;

/**
 * Test data builder utility class for authentication integration tests.
 * Provides factory methods to create test DTOs with sensible defaults.
 * 
 * Usage:
 * RegisterRequestDTO dto = AuthTestDataBuilder.aRegisterRequest()
 *     .withUsername("customuser")
 *     .build();
 */
public class AuthTestDataBuilder {

    // ==================== Register Request ====================
    
    public static RegisterRequestBuilder aRegisterRequest() {
        return new RegisterRequestBuilder();
    }

    public static class RegisterRequestBuilder {
        private String username = "testuser" + System.currentTimeMillis();
        private String password = "TestPassword123";
        private String email = "test" + System.currentTimeMillis() + "@example.com";
        private String firstName = "TestFirst";
        private String lastName = "TestLast";

        public RegisterRequestBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        public RegisterRequestBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public RegisterRequestBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public RegisterRequestBuilder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public RegisterRequestBuilder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public RegisterRequestDTO build() {
            return RegisterRequestDTO.builder()
                    .username(username)
                    .password(password)
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .build();
        }
    }

    // ==================== Authentication Request ====================
    
    public static AuthenticationRequestBuilder aLoginRequest() {
        return new AuthenticationRequestBuilder();
    }

    public static class AuthenticationRequestBuilder {
        private String username = "testuser";
        private String password = "TestPassword123";

        public AuthenticationRequestBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        public AuthenticationRequestBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public AuthenticationRequestDTO build() {
            return AuthenticationRequestDTO.builder()
                    .username(username)
                    .password(password)
                    .build();
        }
    }

    // ==================== Refresh Token Request ====================
    
    public static RefreshTokenRequestBuilder aRefreshTokenRequest() {
        return new RefreshTokenRequestBuilder();
    }

    public static class RefreshTokenRequestBuilder {
        private String refreshToken = "valid.refresh.token";

        public RefreshTokenRequestBuilder withRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public RefreshTokenRequestDTO build() {
            return new RefreshTokenRequestDTO(refreshToken);
        }
    }

    // ==================== Test Constants ====================
    
    public static final class TestConstants {
        public static final String VALID_USERNAME = "validuser";
        public static final String VALID_PASSWORD = "ValidPassword123";
        public static final String VALID_EMAIL = "valid@example.com";
        public static final String VALID_FIRST_NAME = "Valid";

        public static final String INVALID_EMAIL = "invalid-email";
        public static final String SHORT_USERNAME = "ab";
        public static final String WEAK_PASSWORD = "weak";
        public static final String LONG_USERNAME = "a".repeat(51);

        public static final String MIN_VALID_USERNAME = "abc";
        public static final String MIN_VALID_PASSWORD = "Pass1234";
        public static final String MAX_VALID_USERNAME = "a".repeat(50);
        public static final String MAX_VALID_PASSWORD = "a".repeat(50);

        private TestConstants() {
            // Utility class
        }
    }

    // ==================== Valid Test Data Sets ====================
    
    public static final class ValidTestData {
        public static final RegisterRequestDTO VALID_REGISTRATION = RegisterRequestDTO.builder()
                .username("valid_test_user")
                .password("ValidPassword123")
                .email("valid_test@example.com")
                .firstName("ValidTest")
                .lastName("User")
                .build();

        public static final AuthenticationRequestDTO VALID_LOGIN = AuthenticationRequestDTO.builder()
                .username("valid_test_user")
                .password("ValidPassword123")
                .build();

        private ValidTestData() {
            // Utility class
        }
    }

    // ==================== Invalid Test Data Sets ====================
    
    public static final class InvalidTestData {
        public static final RegisterRequestDTO INVALID_EMAIL = RegisterRequestDTO.builder()
                .username("testuser")
                .password("TestPassword123")
                .email("invalid-email")
                .firstName("Test")
                .lastName("User")
                .build();

        public static final RegisterRequestDTO SHORT_USERNAME = RegisterRequestDTO.builder()
                .username("ab")
                .password("TestPassword123")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        public static final RegisterRequestDTO WEAK_PASSWORD = RegisterRequestDTO.builder()
                .username("testuser")
                .password("weak")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        public static final AuthenticationRequestDTO SHORT_LOGIN_USERNAME = AuthenticationRequestDTO.builder()
                .username("ab")
                .password("TestPassword123")
                .build();

        public static final AuthenticationRequestDTO WEAK_LOGIN_PASSWORD = AuthenticationRequestDTO.builder()
                .username("testuser")
                .password("weak")
                .build();

        private InvalidTestData() {
            // Utility class
        }
    }
}
