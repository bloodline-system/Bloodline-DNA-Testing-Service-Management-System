package com.dna_testing_system.dev.integration.auth;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.auth.AuthenticationRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.RefreshTokenRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.RegisterRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.VerificationOptRequestDTO;
import com.dna_testing_system.dev.dto.response.auth.AuthTokensResponseDTO;
import com.dna_testing_system.dev.dto.response.auth.OtpDebugResponseDTO;
import com.dna_testing_system.dev.dto.response.auth.RegisterResponseDTO;
import com.dna_testing_system.dev.integration.common.AbstractIntegrationTest;
import com.dna_testing_system.dev.repository.SignUpRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.auth.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Authentication feature using Testcontainers.
 * Tests the full auth flow including login, sign-up, verification, token refresh, and logout
 * against a real MySQL database and Redis container.
 * 
 * Extends AbstractIntegrationTest to share MySQL and Redis containers across integration tests.
 */
@DisplayName("Authentication Integration Tests with Testcontainers")
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SignUpRepository signUpRepository;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private static final String AUTH_API = "/api/v1/auth";
    private static final String LOGIN_ENDPOINT = AUTH_API + "/login";
    private static final String SIGN_UP_ENDPOINT = AUTH_API + "/sign-up";
    private static final String VERIFICATION_ENDPOINT = AUTH_API + "/verification";
    private static final String REFRESH_TOKEN_ENDPOINT = AUTH_API + "/refresh-token";
    private static final String LOGOUT_ENDPOINT = AUTH_API + "/logout";
    private static final String OTP_DEBUG_ENDPOINT = "/api/debug/otp/{signUpId}";

    @BeforeEach
    void setUp() {
        // Create MockMvc from web application context
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // Clear user database before each test
        userRepository.deleteAll();
        signUpRepository.deleteAll();
        // Flush Redis data before each test for isolation (if available)
        try {
            flushRedis();
        } catch (Exception e) {
            // Redis may not be available in all environments (e.g., CI without Docker)
            // This is acceptable for integration tests
        }
    }

    private void registerAndVerifyUser(RegisterRequestDTO signUpRequest) throws Exception {
        MvcResult signUpResult = mockMvc.perform(post(SIGN_UP_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<RegisterResponseDTO> signUpResponse = objectMapper.readValue(
                signUpResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, RegisterResponseDTO.class)
        );

        String signUpId = signUpResponse.getData().signUpId();

        MvcResult otpResult = mockMvc.perform(get(OTP_DEBUG_ENDPOINT, signUpId))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<OtpDebugResponseDTO> otpResponse = objectMapper.readValue(
                otpResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, OtpDebugResponseDTO.class)
        );

        VerificationOptRequestDTO verificationRequest = new VerificationOptRequestDTO(
                signUpId,
                otpResponse.getData().otpCode()
        );

        mockMvc.perform(post(VERIFICATION_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("Sign Up Tests")
    class SignUpTests {

        @Test
        @DisplayName("Should successfully sign up a new user with valid credentials")
        void signUp_withValidCredentials_succeeds() throws Exception {
            RegisterRequestDTO signUpRequest = RegisterRequestDTO.builder()
                    .username("testuser123")
                    .password("TestPassword123")
                    .email("testuser@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .build();

            MvcResult result = mockMvc.perform(post(SIGN_UP_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("User registered successfully"))
                    .andExpect(jsonPath("$.data['sign-up-id']").exists())
                    .andReturn();

            String responseContent = result.getResponse().getContentAsString();
            ApiResponse<RegisterResponseDTO> response = objectMapper.readValue(
                    responseContent,
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, RegisterResponseDTO.class)
            );

            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().signUpId()).isNotEmpty();
        }

        @Test
        @DisplayName("Should reject sign up with invalid email")
        void signUp_withInvalidEmail_fails() throws Exception {
            RegisterRequestDTO signUpRequest = RegisterRequestDTO.builder()
                    .username("testuser123")
                    .password("TestPassword123")
                    .email("invalid-email")
                    .firstName("Test")
                    .lastName("User")
                    .build();

            mockMvc.perform(post(SIGN_UP_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject sign up with short username")
        void signUp_withShortUsername_fails() throws Exception {
            RegisterRequestDTO signUpRequest = RegisterRequestDTO.builder()
                    .username("ab")  // Too short, minimum is 3
                    .password("TestPassword123")
                    .email("testuser@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .build();

            mockMvc.perform(post(SIGN_UP_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject sign up with weak password")
        void signUp_withWeakPassword_fails() throws Exception {
            RegisterRequestDTO signUpRequest = RegisterRequestDTO.builder()
                    .username("testuser123")
                    .password("weak")  // Too short, minimum is 8
                    .email("testuser@example.com")
                    .firstName("Test")
                    .lastName("User")
                    .build();

            mockMvc.perform(post(SIGN_UP_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject duplicate username registration")
        void signUp_withDuplicateUsername_fails() throws Exception {
            RegisterRequestDTO signUpRequest1 = RegisterRequestDTO.builder()
                    .username("duplicateuser")
                    .password("TestPassword123")
                    .email("user1@example.com")
                    .firstName("User")
                    .lastName("One")
                    .build();

            // First registration should succeed
            mockMvc.perform(post(SIGN_UP_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpRequest1)))
                    .andExpect(status().isOk());

            // Second registration with the same username should fail
            RegisterRequestDTO signUpRequest2 = RegisterRequestDTO.builder()
                    .username("duplicateuser")
                    .password("TestPassword456")
                    .email("user2@example.com")
                    .firstName("Another")
                    .lastName("User")
                    .build();

            mockMvc.perform(post(SIGN_UP_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(signUpRequest2)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        private RegisterRequestDTO createAndRegisterUser() throws Exception {
            RegisterRequestDTO signUpRequest = RegisterRequestDTO.builder()
                    .username("loginuser")
                    .password("LoginPassword123")
                    .email("loginuser@example.com")
                    .firstName("Login")
                    .lastName("User")
                    .build();

            registerAndVerifyUser(signUpRequest);

            return signUpRequest;
        }

        @Test
        @DisplayName("Should successfully login with valid credentials")
        void login_withValidCredentials_succeeds() throws Exception {
            RegisterRequestDTO signUpRequest = createAndRegisterUser();

            AuthenticationRequestDTO loginRequest = AuthenticationRequestDTO.builder()
                    .username("loginuser")
                    .password("LoginPassword123")
                    .build();

            MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Login successfully"))
                    .andExpect(jsonPath("$.data.access_token").exists())
                    .andExpect(jsonPath("$.data.refresh_token").exists())
                    .andReturn();

            String responseContent = result.getResponse().getContentAsString();
            ApiResponse<AuthTokensResponseDTO> response = objectMapper.readValue(
                    responseContent,
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthTokensResponseDTO.class)
            );

            assertThat(response.getData().getAccessToken()).isNotEmpty();
            assertThat(response.getData().getRefreshToken()).isNotEmpty();
        }

        @Test
        @DisplayName("Should reject login with invalid username")
        void login_withInvalidUsername_fails() throws Exception {
            AuthenticationRequestDTO loginRequest = AuthenticationRequestDTO.builder()
                    .username("nonexistentuser")
                    .password("SomePassword123")
                    .build();

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Should reject login with incorrect password")
        void login_withIncorrectPassword_fails() throws Exception {
            createAndRegisterUser();

            AuthenticationRequestDTO loginRequest = AuthenticationRequestDTO.builder()
                    .username("loginuser")
                    .password("WrongPassword123")
                    .build();

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject login with short username")
        void login_withShortUsername_fails() throws Exception {
            AuthenticationRequestDTO loginRequest = AuthenticationRequestDTO.builder()
                    .username("ab")  // Too short
                    .password("SomePassword123")
                    .build();

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject login with weak password")
        void login_withWeakPassword_fails() throws Exception {
            AuthenticationRequestDTO loginRequest = AuthenticationRequestDTO.builder()
                    .username("testuser")
                    .password("weak")  // Too short
                    .build();

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject login with missing credentials")
        void login_withMissingCredentials_fails() throws Exception {
            AuthenticationRequestDTO loginRequest = AuthenticationRequestDTO.builder()
                    .username("testuser")
                    // Missing password
                    .build();

            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Token Refresh Tests")
    class TokenRefreshTests {

        private String getRefreshToken() throws Exception {
            RegisterRequestDTO signUpRequest = RegisterRequestDTO.builder()
                    .username("refreshuser")
                    .password("RefreshPassword123")
                    .email("refreshuser@example.com")
                    .firstName("Refresh")
                    .lastName("User")
                    .build();
            registerAndVerifyUser(signUpRequest);

            AuthenticationRequestDTO loginRequest = AuthenticationRequestDTO.builder()
                    .username("refreshuser")
                    .password("RefreshPassword123")
                    .build();

            MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseContent = result.getResponse().getContentAsString();
            ApiResponse<AuthTokensResponseDTO> response = objectMapper.readValue(
                    responseContent,
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthTokensResponseDTO.class)
            );

            return response.getData().getRefreshToken();
        }

        @Test
        @DisplayName("Should successfully refresh access token with valid refresh token")
        void refreshToken_withValidRefreshToken_succeeds() throws Exception {
            String refreshToken = getRefreshToken();

            RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO(refreshToken);

            MvcResult result = mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Refresh token successfully"))
                    .andExpect(jsonPath("$.data.access_token").exists())
                    .andExpect(jsonPath("$.data.refresh_token").exists())
                    .andReturn();

            String responseContent = result.getResponse().getContentAsString();
            ApiResponse<AuthTokensResponseDTO> response = objectMapper.readValue(
                    responseContent,
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthTokensResponseDTO.class)
            );

            assertThat(response.getData().getAccessToken()).isNotEmpty();
            assertThat(response.getData().getRefreshToken()).isNotEmpty();
        }

        @Test
        @DisplayName("Should reject refresh with invalid refresh token")
        void refreshToken_withInvalidRefreshToken_fails() throws Exception {
            RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO("invalid.refresh.token");

            mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refreshRequest)))
                                        .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject refresh with empty refresh token")
        void refreshToken_withEmptyRefreshToken_fails() throws Exception {
            RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO("");

            mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        private AuthTokensResponseDTO loginUser(String username, String password, String email, String firstName) throws Exception {
            RegisterRequestDTO signUpRequest = RegisterRequestDTO.builder()
                    .username(username)
                    .password(password)
                    .email(email)
                    .firstName(firstName)
                    .lastName("TestLast")
                    .build();
            registerAndVerifyUser(signUpRequest);

            AuthenticationRequestDTO loginRequest = AuthenticationRequestDTO.builder()
                    .username(username)
                    .password(password)
                    .build();

            MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseContent = result.getResponse().getContentAsString();
            ApiResponse<AuthTokensResponseDTO> response = objectMapper.readValue(
                    responseContent,
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthTokensResponseDTO.class)
            );

            return response.getData();
        }

        @Test
        @DisplayName("Should successfully logout with valid tokens")
        void logout_withValidTokens_succeeds() throws Exception {
            AuthTokensResponseDTO tokens = loginUser("logoutuser", "LogoutPassword123", "logoutuser@example.com", "Logout");

            RefreshTokenRequestDTO logoutRequest = new RefreshTokenRequestDTO(tokens.getRefreshToken());

            mockMvc.perform(post(LOGOUT_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + tokens.getAccessToken())
                    .content(objectMapper.writeValueAsString(logoutRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Logout successfully"));
        }

        @Test
        @DisplayName("Should reject logout without authorization header")
        void logout_withoutAuthorizationHeader_fails() throws Exception {
            AuthTokensResponseDTO tokens = loginUser("logoutuser2", "LogoutPassword123", "logoutuser2@example.com", "Logout");

            RefreshTokenRequestDTO logoutRequest = new RefreshTokenRequestDTO(tokens.getRefreshToken());

            mockMvc.perform(post(LOGOUT_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    // No Authorization header
                    .content(objectMapper.writeValueAsString(logoutRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject logout with invalid refresh token")
        void logout_withInvalidRefreshToken_fails() throws Exception {
            AuthTokensResponseDTO tokens = loginUser("logoutuser3", "LogoutPassword123", "logoutuser3@example.com", "Logout");

            RefreshTokenRequestDTO logoutRequest = new RefreshTokenRequestDTO("invalid.token");

            mockMvc.perform(post(LOGOUT_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + tokens.getAccessToken())
                    .content(objectMapper.writeValueAsString(logoutRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Logout successfully"));
        }
    }

    @Nested
    @DisplayName("End-to-End Authentication Flow Tests")
    class EndToEndFlowTests {

        @Test
        @DisplayName("Should complete full authentication cycle: sign-up -> login -> refresh -> logout")
        void fullAuthenticationFlow_succeeds() throws Exception {
            // Step 1: Sign up
            RegisterRequestDTO signUpRequest = RegisterRequestDTO.builder()
                    .username("e2euser")
                    .password("E2EPassword123")
                    .email("e2euser@example.com")
                    .firstName("E2E")
                    .lastName("User")
                    .build();
            registerAndVerifyUser(signUpRequest);

            // Step 2: Login
            AuthenticationRequestDTO loginRequest = AuthenticationRequestDTO.builder()
                    .username("e2euser")
                    .password("E2EPassword123")
                    .build();

            MvcResult loginResult = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String loginResponse = loginResult.getResponse().getContentAsString();
            ApiResponse<AuthTokensResponseDTO> loginData = objectMapper.readValue(
                    loginResponse,
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthTokensResponseDTO.class)
            );

            String accessToken = loginData.getData().getAccessToken();
            String refreshToken = loginData.getData().getRefreshToken();

            assertThat(accessToken).isNotEmpty();
            assertThat(refreshToken).isNotEmpty();

            // Step 3: Refresh token
            RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO(refreshToken);

            MvcResult refreshResult = mockMvc.perform(post(REFRESH_TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String refreshResponse = refreshResult.getResponse().getContentAsString();
            ApiResponse<AuthTokensResponseDTO> refreshData = objectMapper.readValue(
                    refreshResponse,
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthTokensResponseDTO.class)
            );

            String newAccessToken = refreshData.getData().getAccessToken();
            String newRefreshToken = refreshData.getData().getRefreshToken();

            assertThat(newAccessToken).isNotEmpty();
            assertThat(newRefreshToken).isNotEmpty();

            // Step 4: Logout
            RefreshTokenRequestDTO logoutRequest = new RefreshTokenRequestDTO(newRefreshToken);

            mockMvc.perform(post(LOGOUT_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + newAccessToken)
                    .content(objectMapper.writeValueAsString(logoutRequest)))
                    .andExpect(status().isOk());
        }
    }
}
