package com.dna_testing_system.dev.controller.auth;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.auth.AuthenticationRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.RefreshTokenRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.RegisterRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.VerificationOptRequestDTO;
import com.dna_testing_system.dev.dto.response.auth.AuthTokensResponseDTO;
import com.dna_testing_system.dev.dto.response.auth.RegisterResponseDTO;
import com.dna_testing_system.dev.service.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;


    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AuthTokensResponseDTO> login(@Valid @RequestBody AuthenticationRequestDTO loginRequestDTO) {
        AuthTokensResponseDTO response = authenticationService.login(loginRequestDTO);

        return ApiResponse.success(HttpStatus.OK.value(), "Login successfully", response);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequestDTO refreshTokenDTO,
                                    @RequestHeader("Authorization") String authHeader) {
        final String accessToken = extractBearerToken(authHeader);
        final String refreshToken = refreshTokenDTO.refreshToken();
        authenticationService.logout(accessToken, refreshToken);
        return ApiResponse.success(HttpStatus.OK.value(), "Logout successfully", null);
    }


    @PostMapping("/refresh-token")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AuthTokensResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO refreshToken) {
        AuthTokensResponseDTO response = authenticationService.refreshToken(refreshToken);
        return ApiResponse.success(HttpStatus.OK.value(), "Refresh token successfully", response);
    }


    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<RegisterResponseDTO> signUp(@Valid @RequestBody RegisterRequestDTO registerDTO) {
        RegisterResponseDTO response = authenticationService.register(registerDTO);
        return ApiResponse.success(HttpStatus.OK.value(), "User registered successfully", response);
    }


    @PostMapping("/verification")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> verifyUser(@Valid @RequestBody VerificationOptRequestDTO verificationOptRequestDTO) {
            authenticationService.verifyUserRegistration(verificationOptRequestDTO);
        return ApiResponse.success(HttpStatus.OK.value(), "User verified successfully", null);
    }

    @PostMapping("/resend-otp")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> refreshUserVerification(@RequestParam("signUpId") String signUpId) {
        authenticationService.resendUserVerificationOTP(signUpId);
        return ApiResponse.success(HttpStatus.OK.value(), "User verification email sent successfully", null);
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return "";
        }

        final String prefix = "Bearer ";
        if (authHeader.startsWith(prefix)) {
            return authHeader.substring(prefix.length());
        }

        return authHeader;
    }
}