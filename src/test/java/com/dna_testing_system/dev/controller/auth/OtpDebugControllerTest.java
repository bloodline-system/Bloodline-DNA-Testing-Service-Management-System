package com.dna_testing_system.dev.controller.auth;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.dto.response.auth.OtpDebugResponseDTO;
import com.dna_testing_system.dev.exception.OptFailException;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.auth.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OtpDebugController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class OtpDebugControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthenticationService authenticationService;

    @Test
    void getOtpDebugReturnsOkWhenSignUpIdExists() throws Exception {
        when(authenticationService.getOtpForDebugging("s1"))
                .thenReturn(OtpDebugResponseDTO.builder().otpCode("654321").build());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/debug/otp/{signUpId}", "s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OTP code retrieved successfully"))
                .andExpect(jsonPath("$.data.otp_code").value("654321"));
    }

    @Test
    void getOtpDebugReturns400WhenServiceThrowsBadRequestException() throws Exception {
        when(authenticationService.getOtpForDebugging("missing"))
                .thenThrow(new OptFailException("Invalid sign-up ID"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/debug/otp/{signUpId}", "missing"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid sign-up ID"));
    }
}
