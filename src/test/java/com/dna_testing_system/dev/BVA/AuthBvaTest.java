package com.dna_testing_system.dev.BVA;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.controller.auth.AuthController;
import com.dna_testing_system.dev.dto.response.auth.AuthTokensResponseDTO;
import com.dna_testing_system.dev.dto.response.auth.RegisterResponseDTO;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.auth.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthBvaTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthenticationService authenticationService;

    // ==================== LOGIN - Username BVA ====================

    @Test
    void login_usernameLength2_returns400() throws Exception {
        String username2 = "ab";
        String json = "{\"username\":\"" + username2 + "\",\"password\":\"password123\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any());
    }

    @Test
    void login_usernameLength3_returns200() throws Exception {
        mockLoginSuccess();
        String username3 = "abc";
        String json = "{\"username\":\"" + username3 + "\",\"password\":\"password123\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void login_usernameLength50_returns200() throws Exception {
        mockLoginSuccess();
        String username50 = "a".repeat(50);
        String json = "{\"username\":\"" + username50 + "\",\"password\":\"password123\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void login_usernameLength51_returns400() throws Exception {
        String username51 = "a".repeat(51);
        String json = "{\"username\":\"" + username51 + "\",\"password\":\"password123\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any());
    }

    @Test
    void login_usernameBlank_returns400() throws Exception {
        String json = "{\"username\":\"\",\"password\":\"password123\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any());
    }

    // ==================== LOGIN - Password BVA ====================

    @Test
    void login_passwordLength7_returns400() throws Exception {
        String password7 = "p".repeat(7);
        String json = "{\"username\":\"validuser\",\"password\":\"" + password7 + "\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any());
    }

    @Test
    void login_passwordLength8_returns200() throws Exception {
        mockLoginSuccess();
        String password8 = "p".repeat(8);
        String json = "{\"username\":\"validuser\",\"password\":\"" + password8 + "\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void login_passwordLength50_returns200() throws Exception {
        mockLoginSuccess();
        String password50 = "p".repeat(50);
        String json = "{\"username\":\"validuser\",\"password\":\"" + password50 + "\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void login_passwordLength51_returns400() throws Exception {
        String password51 = "p".repeat(51);
        String json = "{\"username\":\"validuser\",\"password\":\"" + password51 + "\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any());
    }

    @Test
    void login_passwordBlank_returns400() throws Exception {
        String json = "{\"username\":\"validuser\",\"password\":\"\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any());
    }

    // ==================== REGISTER - Username BVA ====================

    @Test
    void register_usernameLength2_returns400() throws Exception {
        String username2 = "ab";
        String json = buildRegisterJson(username2, "password123", "test@example.com", "John", "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    @Test
    void register_usernameLength3_returns200() throws Exception {
        mockRegisterSuccess();
        String username3 = "abc";
        String json = buildRegisterJson(username3, "password123", "test@example.com", "John", "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void register_usernameLength50_returns200() throws Exception {
        mockRegisterSuccess();
        String username50 = "a".repeat(50);
        String json = buildRegisterJson(username50, "password123", "test@example.com", "John", "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void register_usernameLength51_returns400() throws Exception {
        String username51 = "a".repeat(51);
        String json = buildRegisterJson(username51, "password123", "test@example.com", "John", "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    // ==================== REGISTER - Password BVA ====================

    @Test
    void register_passwordLength7_returns400() throws Exception {
        String password7 = "p".repeat(7);
        String json = buildRegisterJson("validuser", password7, "test@example.com", "John", "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    @Test
    void register_passwordLength8_returns200() throws Exception {
        mockRegisterSuccess();
        String password8 = "p".repeat(8);
        String json = buildRegisterJson("validuser", password8, "test@example.com", "John", "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void register_passwordLength50_returns200() throws Exception {
        mockRegisterSuccess();
        String password50 = "p".repeat(50);
        String json = buildRegisterJson("validuser", password50, "test@example.com", "John", "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void register_passwordLength51_returns400() throws Exception {
        String password51 = "p".repeat(51);
        String json = buildRegisterJson("validuser", password51, "test@example.com", "John", "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    // ==================== REGISTER - FirstName BVA ====================

    @Test
    void register_firstNameLength100_returns200() throws Exception {
        mockRegisterSuccess();
        String firstName100 = "a".repeat(100);
        String json = buildRegisterJson("validuser", "password123", "test@example.com", firstName100, "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void register_firstNameLength101_returns400() throws Exception {
        String firstName101 = "a".repeat(101);
        String json = buildRegisterJson("validuser", "password123", "test@example.com", firstName101, "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    @Test
    void register_firstNameBlank_returns400() throws Exception {
        String json = buildRegisterJson("validuser", "password123", "test@example.com", "", "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    // ==================== REGISTER - LastName BVA ====================

    @Test
    void register_lastNameLength100_returns200() throws Exception {
        mockRegisterSuccess();
        String lastName100 = "b".repeat(100);
        String json = buildRegisterJson("validuser", "password123", "test@example.com", "John", lastName100);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void register_lastNameLength101_returns400() throws Exception {
        String lastName101 = "b".repeat(101);
        String json = buildRegisterJson("validuser", "password123", "test@example.com", "John", lastName101);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    @Test
    void register_lastNameBlank_returns400() throws Exception {
        String json = buildRegisterJson("validuser", "password123", "test@example.com", "John", "");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    // ==================== REGISTER - Email BVA ====================

    @Test
    void register_emailInvalidFormat_returns400() throws Exception {
        String json = buildRegisterJson("validuser", "password123", "invalid-email", "John", "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    @Test
    void register_emailBlank_returns400() throws Exception {
        String json = buildRegisterJson("validuser", "password123", "", "John", "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    @Test
    void register_emailValidFormat_returns200() throws Exception {
        mockRegisterSuccess();
        String json = buildRegisterJson("validuser", "password123", "valid@example.com", "John", "Doe");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== REFRESH TOKEN BVA ====================

    @Test
    void refreshToken_tokenBlank_returns400() throws Exception {
        String json = "{\"refreshToken\":\"\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).refreshToken(any());
    }

    @Test
    void refreshToken_tokenValid_returns200() throws Exception {
        mockRefreshTokenSuccess();
        String json = "{\"refreshToken\":\"valid.refresh.token\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== VERIFICATION OTP BVA ====================

    @Test
    void verifyOtp_signUpIdBlank_returns400() throws Exception {
        String json = "{\"signUpId\":\"\",\"otp\":\"123456\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).verifyUserRegistration(any());
    }

    @Test
    void verifyOtp_otpBlank_returns400() throws Exception {
        String json = "{\"signUpId\":\"valid-signup-id\",\"otp\":\"\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).verifyUserRegistration(any());
    }

    @Test
    void verifyOtp_otpLength5_returns400() throws Exception {
        String otp5 = "12345";
        String json = "{\"signUpId\":\"valid-signup-id\",\"otp\":\"" + otp5 + "\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).verifyUserRegistration(any());
    }

    @Test
    void verifyOtp_otpLength6_returns200() throws Exception {
        String otp6 = "123456";
        String json = "{\"signUpId\":\"valid-signup-id\",\"otp\":\"" + otp6 + "\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void verifyOtp_otpLength7_returns400() throws Exception {
        String otp7 = "1234567";
        String json = "{\"signUpId\":\"valid-signup-id\",\"otp\":\"" + otp7 + "\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).verifyUserRegistration(any());
    }

    @Test
    void verifyOtp_otpNonNumeric_returns400() throws Exception {
        String otpAlpha = "abcdef";
        String json = "{\"signUpId\":\"valid-signup-id\",\"otp\":\"" + otpAlpha + "\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).verifyUserRegistration(any());
    }

    @Test
    void verifyOtp_otpMixedAlphaNumeric_returns400() throws Exception {
        String otpMixed = "123abc";
        String json = "{\"signUpId\":\"valid-signup-id\",\"otp\":\"" + otpMixed + "\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).verifyUserRegistration(any());
    }

    // ==================== Helper Methods ====================

    private void mockLoginSuccess() {
        AuthTokensResponseDTO response = AuthTokensResponseDTO.builder()
                .accessToken("mock.access.token")
                .refreshToken("mock.refresh.token")
                .expiresIn(System.currentTimeMillis() + 3600000)
                .build();
        when(authenticationService.login(any())).thenReturn(response);
    }

    private void mockRegisterSuccess() {
        RegisterResponseDTO response = RegisterResponseDTO.builder()
                .signUpId("mock-signup-id")
                .build();
        when(authenticationService.register(any())).thenReturn(response);
    }

    private void mockRefreshTokenSuccess() {
        AuthTokensResponseDTO response = AuthTokensResponseDTO.builder()
                .accessToken("new.access.token")
                .refreshToken("new.refresh.token")
                .expiresIn(System.currentTimeMillis() + 3600000)
                .build();
        when(authenticationService.refreshToken(any())).thenReturn(response);
    }

    private String buildRegisterJson(String username, String password, String email, String firstName, String lastName) {
        return String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"email\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\"}",
                username, password, email, firstName, lastName
        );
    }
}
