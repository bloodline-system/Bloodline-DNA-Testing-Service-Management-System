package com.dna_testing_system.dev.controller.auth;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.dto.request.auth.AuthenticationRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.RefreshTokenRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.RegisterRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.VerificationOptRequestDTO;
import com.dna_testing_system.dev.dto.response.auth.AuthTokensResponseDTO;
import com.dna_testing_system.dev.dto.response.auth.RegisterResponseDTO;
import com.dna_testing_system.dev.exception.InvalidTokenException;
import com.dna_testing_system.dev.exception.LoginNotValidException;
import com.dna_testing_system.dev.exception.OptFailException;
import com.dna_testing_system.dev.exception.SignUpNotValidException;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.auth.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    AuthenticationService authenticationService;

    @Test
    void loginReturnsOkWhenRequestIsValid() throws Exception {
        AuthenticationRequestDTO request = AuthenticationRequestDTO.builder()
                .username("alice")
                .password("password123")
                .build();
        AuthTokensResponseDTO response = AuthTokensResponseDTO.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .expiresIn(12345L)
                .userId("u1")
                .build();

        when(authenticationService.login(any(AuthenticationRequestDTO.class))).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Login successfully"))
                .andExpect(jsonPath("$.data.access_token").value("access"))
                .andExpect(jsonPath("$.data.refresh_token").value("refresh"))
                .andExpect(jsonPath("$.data.user_id").value("u1"));
    }

    @Test
    void loginReturns400WhenValidationFails() throws Exception {
        AuthenticationRequestDTO request = AuthenticationRequestDTO.builder()
                .username("ab")
                .password("123")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.username", containsString("between 3 and 50")))
                .andExpect(jsonPath("$.data.password", containsString("between 8 and 50")));
    }

    @Test
    void loginReturns400WhenServiceThrowsBadRequestException() throws Exception {
        AuthenticationRequestDTO request = AuthenticationRequestDTO.builder()
                .username("alice")
                .password("password123")
                .build();

        when(authenticationService.login(any(AuthenticationRequestDTO.class)))
                .thenThrow(new LoginNotValidException("Invalid password"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid password"));
    }

    @Test
    void logoutReturnsOkAndExtractsBearerToken() throws Exception {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("refresh-token");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Logout successfully"));

        verify(authenticationService).logout("access-token", "refresh-token");
    }

    @Test
    void logoutUsesRawAuthorizationWhenNoBearerPrefix() throws Exception {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("refresh-token");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/logout")
                        .header("Authorization", "raw-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk());

        verify(authenticationService).logout("raw-access", "refresh-token");
    }

    @Test
    void logoutPassesEmptyTokenWhenAuthorizationHeaderIsBlank() throws Exception {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("refresh-token");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/logout")
                        .header("Authorization", " ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk());

        verify(authenticationService).logout("", "refresh-token");
    }

    @Test
    void logoutReturns400WhenAuthorizationHeaderMissing() throws Exception {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("refresh-token");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(status().reason("Required header 'Authorization' is not present."));
    }

    @Test
    void logoutReturns400WhenRefreshTokenValidationFails() throws Exception {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(" ");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void logoutReturns400WhenServiceThrowsBadRequestException() throws Exception {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("refresh-token");
        doThrow(new InvalidTokenException("Access token is required"))
                .when(authenticationService)
                .logout("access-token", "refresh-token");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Access token is required"));
    }

    @Test
    void refreshTokenReturnsOkWhenRequestIsValid() throws Exception {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("refresh-token");
        AuthTokensResponseDTO response = AuthTokensResponseDTO.builder()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .expiresIn(9999L)
                .userId("u1")
                .build();

        when(authenticationService.refreshToken(any(RefreshTokenRequestDTO.class))).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Refresh token successfully"))
                .andExpect(jsonPath("$.data.access_token").value("new-access"));
    }

    @Test
    void refreshTokenReturns400WhenValidationFails() throws Exception {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(" ");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void signUpReturnsOkWhenRequestIsValid() throws Exception {
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .username("alice")
                .password("password123")
                .email("alice@example.com")
                .firstName("Alice")
                .lastName("Walker")
                .build();

        when(authenticationService.register(any(RegisterRequestDTO.class)))
                .thenReturn(RegisterResponseDTO.builder().signUpId("s1").build());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.sign-up-id").value("s1"));
    }

    @Test
    void signUpReturns400WhenValidationFails() throws Exception {
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .username("ab")
                .password("123")
                .email("bad-email")
                .firstName(" ")
                .lastName(" ")
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.username", containsString("between 3 and 50")))
                .andExpect(jsonPath("$.data.password", containsString("between 8 and 50")))
                .andExpect(jsonPath("$.data.email", containsString("must be valid")));
    }

    @Test
    void signUpReturns400WhenServiceThrowsBadRequestException() throws Exception {
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .username("alice")
                .password("password123")
                .email("alice@example.com")
                .firstName("Alice")
                .lastName("Walker")
                .build();

        when(authenticationService.register(any(RegisterRequestDTO.class)))
                .thenThrow(new SignUpNotValidException("Username already exists"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void verifyUserReturnsOkWhenRequestIsValid() throws Exception {
        VerificationOptRequestDTO request = new VerificationOptRequestDTO("s1", "123456");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User verified successfully"));

        verify(authenticationService).verifyUserRegistration(request);
    }

    @Test
    void verifyUserReturns400WhenOtpPatternInvalid() throws Exception {
        VerificationOptRequestDTO request = new VerificationOptRequestDTO("s1", "12A");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.otp", containsString("OTP must be 6 digits")));
    }

    @Test
    void verifyUserReturns400WhenServiceThrowsBadRequestException() throws Exception {
        VerificationOptRequestDTO request = new VerificationOptRequestDTO("s1", "123456");
        doThrow(new OptFailException("Verification token has expired"))
                .when(authenticationService)
                .verifyUserRegistration(any(VerificationOptRequestDTO.class));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Verification token has expired"));
    }

    @Test
    void resendOtpReturnsOkWhenSignUpIdProvided() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/resend-otp")
                        .param("signUpId", "s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User verification email sent successfully"));

        verify(authenticationService).resendUserVerificationOTP("s1");
    }

    @Test
    void resendOtpReturns400WhenSignUpIdMissing() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/resend-otp"))
                                .andExpect(status().isBadRequest())
                                .andExpect(status().reason("Required parameter 'signUpId' is not present."));
    }

    @Test
    void resendOtpReturns400WhenServiceThrowsBadRequestException() throws Exception {
        doThrow(new OptFailException("Please wait 60 seconds before requesting another OTP"))
                .when(authenticationService)
                .resendUserVerificationOTP("s1");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/resend-otp")
                        .param("signUpId", "s1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Please wait 60 seconds before requesting another OTP"));
    }
}
