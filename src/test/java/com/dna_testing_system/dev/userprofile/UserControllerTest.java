package com.dna_testing_system.dev.userprofile;

import com.dna_testing_system.dev.controller.UserController;
import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.dto.request.UserProfileRequest;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
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
        controllers = UserController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

        @MockitoBean
    UserProfileService userProfileService;

    // Required constructor deps (not used directly in these endpoints)
        @MockitoBean OrderService orderService;
        @MockitoBean OrderKitService orderKitService;
        @MockitoBean OrderParticipantService orderParticipantService;
        @MockitoBean UserService userService;
        @MockitoBean StaffService staffService;
        @MockitoBean MedicalServiceManageService medicalService;
        @MockitoBean TestKitService testKitService;

    @Test
    void getAllProfilesReturnsOkWithList() throws Exception {
        when(userProfileService.getUserProfiles()).thenReturn(List.of(
                UserProfileResponse.builder().username("alice").email("a@ex.com").build(),
                UserProfileResponse.builder().username("bob").email("b@ex.com").build()
        ));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/profiles/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Get all profiles successfully"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].username").value("alice"));

        verify(userProfileService).getUserProfiles();
    }

    @Test
    void searchProfilesReturnsOk() throws Exception {
        when(userProfileService.getUserProfileByName("Ali")).thenReturn(List.of(
                UserProfileResponse.builder().username("alice").build()
        ));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/profiles/search")
                        .param("name", "Ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Search profiles successfully"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].username").value("alice"));

        verify(userProfileService).getUserProfileByName("Ali");
    }

    @Test
    void getProfileReturnsOkWhenFound() throws Exception {
        when(userProfileService.getUserProfile("alice"))
                .thenReturn(UserProfileResponse.builder().username("alice").email("a@ex.com").build());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/profiles/{username}", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Get profile successfully"))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void getProfileReturns404WhenServiceThrows() throws Exception {
        when(userProfileService.getUserProfile("ghost")).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/profiles/{username}", "ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Profile not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/profiles/ghost"));
    }

    @Test
    void updateProfileKeepsExistingDobAndImageWhenNoFile() throws Exception {
        UserProfileResponse existing = UserProfileResponse.builder()
                .username("alice")
                .email("old@ex.com")
                .profileImageUrl("/uploads/existing.png")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .build();
        UserProfileResponse updated = UserProfileResponse.builder()
                .username("alice")
                .email("new@ex.com")
                .profileImageUrl("/uploads/existing.png")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .build();

        when(userProfileService.getUserProfile("alice")).thenReturn(existing, updated);
        when(userProfileService.updateUserProfile(eq("alice"), org.mockito.ArgumentMatchers.any(UserProfileRequest.class))).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/profiles/{username}", "alice")
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("firstName", "Alice")
                        .param("lastName", "Ng")
                        .param("email", "new@ex.com")
                        .param("phoneNumber", "+84 123")
                                                .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Update profile successfully"))
                .andExpect(jsonPath("$.data.email").value("new@ex.com"));

        ArgumentCaptor<UserProfileRequest> captor = ArgumentCaptor.forClass(UserProfileRequest.class);
        verify(userProfileService).updateUserProfile(eq("alice"), captor.capture());
        UserProfileRequest sent = captor.getValue();
        assertEquals("/uploads/existing.png", sent.getProfileImageUrl());
        assertEquals(LocalDate.of(2000, 1, 1), sent.getDateOfBirth());
    }

    @Test
    void updateProfileReturns404WhenUpdateReturnsFalse() throws Exception {
        when(userProfileService.getUserProfile("ghost"))
                .thenReturn(UserProfileResponse.builder().username("ghost").build());
        when(userProfileService.updateUserProfile(eq("ghost"), org.mockito.ArgumentMatchers.any(UserProfileRequest.class))).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/profiles/{username}", "ghost")
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("email", "x@ex.com")
                                                .param("password", "password123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Profile not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/profiles/ghost"));
    }

    @Test
    void deleteProfileReturnsOkWhenDeleted() throws Exception {
        when(userProfileService.deleteUserProfile("alice")).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/profiles/{username}", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Delete profile successfully"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void deleteProfileReturns404WhenNotDeleted() throws Exception {
        when(userProfileService.deleteUserProfile("ghost")).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/profiles/{username}", "ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Profile not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/profiles/ghost"));
    }
}
