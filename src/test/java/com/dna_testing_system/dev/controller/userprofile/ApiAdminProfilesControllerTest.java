package com.dna_testing_system.dev.controller.userprofile;

import com.dna_testing_system.dev.controller.api.ApiAdminProfilesController;
import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ApiAdminProfilesController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ApiAdminProfilesControllerTest {

    @Autowired
    MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
    UserProfileService userProfileService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllProfilesWithoutQueryUsesGetUserProfiles() throws Exception {
        when(userProfileService.getUserProfiles()).thenReturn(List.of(
                UserProfileResponse.builder().username("alice").build()
        ));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Profiles loaded successfully"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].username").value("alice"));

        verify(userProfileService).getUserProfiles();
        verify(userProfileService, never()).getUserProfileByName(anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllProfilesWithQueryUsesSearch() throws Exception {
        when(userProfileService.getUserProfileByName("Ali")).thenReturn(List.of(
                UserProfileResponse.builder().username("alice").build()
        ));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/profiles")
                        .param("query", "Ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(userProfileService).getUserProfileByName("Ali");
        verify(userProfileService, never()).getUserProfiles();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProfileByUsernameReturns404WhenNull() throws Exception {
        when(userProfileService.getUserProfile("ghost")).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/profiles/{username}", "ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Profile not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProfileReturns404WhenExistingProfileNull() throws Exception {
        when(userProfileService.getUserProfile("ghost")).thenReturn(null);

        MockMultipartFile profilePart = new MockMultipartFile(
                "profile",
                "profile.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(UserProfileRequest.builder().email("x@ex.com").build())
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/admin/profiles/{username}", "ghost")
                        .file(profilePart)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Profile not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProfileKeepsExistingImageAndDobWhenFileMissing() throws Exception {
        UserProfileResponse existing = UserProfileResponse.builder()
                .username("alice")
                .profileImageUrl("/uploads/existing.png")
                .dateOfBirth(LocalDate.of(1999, 12, 31))
                .build();
        UserProfileResponse updated = UserProfileResponse.builder()
                .username("alice")
                .email("new@ex.com")
                .profileImageUrl("/uploads/existing.png")
                .dateOfBirth(LocalDate.of(1999, 12, 31))
                .build();

        when(userProfileService.getUserProfile("alice")).thenReturn(existing, updated);
        when(userProfileService.updateUserProfile(eq("alice"), org.mockito.ArgumentMatchers.any(UserProfileRequest.class))).thenReturn(true);

        UserProfileRequest request = UserProfileRequest.builder()
                .email("new@ex.com")
                .build();
        MockMultipartFile profilePart = new MockMultipartFile(
                "profile",
                "profile.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/admin/profiles/{username}", "alice")
                        .file(profilePart)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data.email").value("new@ex.com"));

        ArgumentCaptor<UserProfileRequest> captor = ArgumentCaptor.forClass(UserProfileRequest.class);
        verify(userProfileService).updateUserProfile(eq("alice"), captor.capture());
        UserProfileRequest sent = captor.getValue();
        assertEquals("/uploads/existing.png", sent.getProfileImageUrl());
        assertEquals(LocalDate.of(1999, 12, 31), sent.getDateOfBirth());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProfileReturnsOkWhenDeleted() throws Exception {
        when(userProfileService.deleteUserProfile("alice")).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/admin/profiles/{username}", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }
}
