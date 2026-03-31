package com.dna_testing_system.dev.BVA;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.controller.UserController;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.OrderKitService;
import com.dna_testing_system.dev.service.OrderParticipantService;
import com.dna_testing_system.dev.service.OrderService;
import com.dna_testing_system.dev.service.UserProfileService;
import com.dna_testing_system.dev.service.service.MedicalServiceManageService;
import com.dna_testing_system.dev.service.staff.StaffService;
import com.dna_testing_system.dev.service.staff.TestKitService;
import com.dna_testing_system.dev.service.user.UserService;
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

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileBvaTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserProfileService userProfileService;

    @MockitoBean
    OrderService orderService;
    @MockitoBean
    OrderKitService orderKitService;
    @MockitoBean
    OrderParticipantService orderParticipantService;
    @MockitoBean
    UserService userService;
    @MockitoBean
    StaffService staffService;
    @MockitoBean
    MedicalServiceManageService medicalService;
    @MockitoBean
    TestKitService testKitService;

    @Test
    void updateProfileJson_firstNameLength100_returns200() throws Exception {
        mockUpdateSuccess("alice");
        String firstName100 = "a".repeat(100);
        String json = "{\"firstName\":\"" + firstName100 + "\",\"email\":\"alice@example.com\"}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/profiles/{username}", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateProfileJson_firstNameLength101_returns400() throws Exception {
        String firstName101 = "a".repeat(101);
        String json = "{\"firstName\":\"" + firstName101 + "\",\"email\":\"alice@example.com\"}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/profiles/{username}", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(userProfileService, never()).updateUserProfile(eq("alice"), any());
    }

        @Test
        void updateProfileJson_lastNameLength100_returns200() throws Exception {
                mockUpdateSuccess("alice");
                String lastName100 = "b".repeat(100);
                String json = "{\"lastName\":\"" + lastName100 + "\",\"email\":\"alice@example.com\"}";

                mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/profiles/{username}", "alice")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        void updateProfileJson_lastNameLength101_returns400() throws Exception {
                String lastName101 = "b".repeat(101);
                String json = "{\"lastName\":\"" + lastName101 + "\",\"email\":\"alice@example.com\"}";

                mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/profiles/{username}", "alice")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(json))
                                .andExpect(status().isBadRequest());

                verify(userProfileService, never()).updateUserProfile(eq("alice"), any());
        }

    @Test
    void updateProfileJson_phoneLength20_returns200() throws Exception {
        mockUpdateSuccess("alice");
        String phone20 = "1".repeat(20);
        String json = "{\"email\":\"alice@example.com\",\"phoneNumber\":\"" + phone20 + "\"}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/profiles/{username}", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateProfileJson_phoneLength21_returns400() throws Exception {
        String phone21 = "1".repeat(21);
        String json = "{\"email\":\"alice@example.com\",\"phoneNumber\":\"" + phone21 + "\"}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/profiles/{username}", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(userProfileService, never()).updateUserProfile(eq("alice"), any());
    }

    @Test
    void updateProfileJson_phonePatternInvalid_returns400() throws Exception {
        String json = "{\"email\":\"alice@example.com\",\"phoneNumber\":\"123abc\"}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/profiles/{username}", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(userProfileService, never()).updateUserProfile(eq("alice"), any());
    }

    @Test
    void updateProfileJson_emailMissing_returns400() throws Exception {
        String json = "{\"firstName\":\"Alice\"}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/profiles/{username}", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(userProfileService, never()).updateUserProfile(eq("alice"), any());
    }

    @Test
    void updateProfileJson_emailInvalidFormat_returns400() throws Exception {
        String json = "{\"email\":\"invalid-email\"}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/profiles/{username}", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(userProfileService, never()).updateUserProfile(eq("alice"), any());
    }

    private void mockUpdateSuccess(String username) {
        UserProfileResponse existing = UserProfileResponse.builder()
                .username(username)
                .email("old@example.com")
                .profileImageUrl("/uploads/current.png")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .build();
        UserProfileResponse updated = UserProfileResponse.builder()
                .username(username)
                .email("alice@example.com")
                .profileImageUrl("/uploads/current.png")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .build();

        when(userProfileService.getUserProfile(username)).thenReturn(existing, updated);
        when(userProfileService.updateUserProfile(eq(username), any())).thenReturn(true);
    }
}
