package com.dna_testing_system.dev.controller.auth;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.response.auth.OtpDebugResponseDTO;
import com.dna_testing_system.dev.service.auth.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/debug")
public class OtpDebugController {
    
    private final AuthenticationService authenticationService;
    @GetMapping("/otp/{signUpId}")
    public ApiResponse<OtpDebugResponseDTO> getOtpDebug(@PathVariable String signUpId) {
        OtpDebugResponseDTO responseDTO = authenticationService.getOtpForDebugging(signUpId);
        return ApiResponse.success(200, "OTP code retrieved successfully", responseDTO);
    }

}
