package com.dna_testing_system.dev.BVA;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.controller.api.ApiAdminProfilesController;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class ApiAdminProfilesFilterBvaTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserProfileService userProfileService;

    // =====================================================================
    // PHAN 1 - query boundary
    // Bien: null, blank, 1-char keyword
    // =====================================================================

    /** BVA-AP-QR-01..02: query null/blank -> getUserProfiles */
    @ParameterizedTest
    @WithMockUser(roles = "ADMIN")
    @ValueSource(strings = {"", "   "})
    void getAllProfiles_queryBlankBoundary_usesGetUserProfiles(String query) throws Exception {
        when(userProfileService.getUserProfiles()).thenReturn(List.of(UserProfileResponse.builder().username("alice").build()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/profiles").param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(userProfileService).getUserProfiles();
        verify(userProfileService, never()).getUserProfileByName(query);
    }

    /** BVA-AP-QR-03: query length=1 -> search branch */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllProfiles_queryLengthOne_usesSearch() throws Exception {
        when(userProfileService.getUserProfileByName("a")).thenReturn(List.of(UserProfileResponse.builder().username("alice").build()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/profiles").param("query", "a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(userProfileService).getUserProfileByName("a");
        verify(userProfileService, never()).getUserProfiles();
    }
}
