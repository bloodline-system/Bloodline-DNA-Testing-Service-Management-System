package com.dna_testing_system.dev.service.auth;


import com.dna_testing_system.dev.dto.request.auth.AuthenticationRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.RefreshTokenRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.RegisterRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.VerificationOptRequestDTO;
import com.dna_testing_system.dev.dto.response.auth.AuthTokensResponseDTO;
import com.dna_testing_system.dev.dto.response.auth.OtpDebugResponseDTO;
import com.dna_testing_system.dev.dto.response.auth.RegisterResponseDTO;

public interface AuthenticationService {
        AuthTokensResponseDTO login(AuthenticationRequestDTO loginRequestDTO);
        void logout(String accessToken, String refreshToken);
        AuthTokensResponseDTO refreshToken(RefreshTokenRequestDTO refreshToken);
        RegisterResponseDTO register(RegisterRequestDTO registerRequestDTO);
        void verifyUserRegistration(VerificationOptRequestDTO verificationOptRequestDTO);
        void resendUserVerificationOTP(String signUpId);

        OtpDebugResponseDTO getOtpForDebugging(String signUpId);
}
