package com.dna_testing_system.dev.BVA;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.ManagerReportController;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.dto.response.SystemReportResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.ReportStatus;
import com.dna_testing_system.dev.enums.ReportType;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.SystemReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ManagerReportController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
})
@AutoConfigureMockMvc(addFilters = true)
class ManagerReportBvaTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SystemReportService systemReportService;

    @MockitoBean
    UserRepository userRepository;

    @TestConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) {
            try {
                http.csrf(csrf -> csrf.disable())
                        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .exceptionHandling(ex -> ex
                                .authenticationEntryPoint((request, response, authException) -> response.setStatus(401))
                                .accessDeniedHandler((request, response, accessDeniedException) -> response.setStatus(403)));
                return http.build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void getAllReports_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/manager/reports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void getAllReports_roleStaff_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/manager/reports"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getAllReports_withReports_returns200() throws Exception {
        User manager = User.builder().id("mgr-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(systemReportService.getAllSystemReports()).thenReturn(List.of(
                SystemReportResponse.builder()
                        .reportId(1L)
                        .reportName("Monthly Report")
                        .reportStatus(ReportStatus.APPROVED)
                        .reportType(ReportType.MONTHLY_REVENUE)
                        .reportCategory("Finance")
                        .reportData("{\"report\":\"data\"}")
                        .build()
        ));

        mockMvc.perform(get("/api/v1/manager/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReports").value(1));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getAllReports_emptyReports_returns200() throws Exception {
        User manager = User.builder().id("mgr-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(systemReportService.getAllSystemReports()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/manager/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReports").value(0));
    }

    @Test
    void getReportById_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/manager/reports/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getReportById_validId_returns200() throws Exception {
        when(systemReportService.getSystemReportByReportId(1L)).thenReturn(
                SystemReportResponse.builder()
                        .reportId(1L)
                        .reportName("Monthly Report")
                        .reportStatus(ReportStatus.APPROVED)
                        .reportType(ReportType.MONTHLY_REVENUE)
                        .reportCategory("Finance")
                        .reportData("{\"report\":\"data\"}")
                        .build()
        );

        mockMvc.perform(get("/api/v1/manager/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(1L));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getReportById_invalidId_returns404() throws Exception {
        when(systemReportService.getSystemReportByReportId(999L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/manager/reports/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReportStatus_noToken_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/manager/reports/1/status")
                        .param("status", "ACTIVE"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updateReportStatus_blankStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/manager/reports/1/status")
                        .contentType("application/json")
                        .content("{\"status\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updateReportStatus_validStatus_returns200() throws Exception {
        User manager = User.builder().id("mgr-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("mgr-1")).thenReturn(Optional.of(manager));
        when(systemReportService.getSystemReportByReportId(1L)).thenReturn(
                SystemReportResponse.builder()
                        .reportId(1L)
                        .reportName("Monthly Report")
                        .reportStatus(ReportStatus.APPROVED)
                        .reportType(ReportType.MONTHLY_REVENUE)
                        .reportCategory("Finance")
                        .reportData("{\"report\":\"data\"}")
                        .build()
        );
        doNothing().when(systemReportService).updateExistReport(any(), eq(1L));

        mockMvc.perform(patch("/api/v1/manager/reports/1/status")
                        .contentType("application/json")
                        .content("{\"status\": \"approved\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void createReport_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/manager/reports")
                        .contentType("application/json")
                        .content("{\"reportName\": \"New Report\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void createReport_validData_returns201() throws Exception {
        User manager = User.builder().id("mgr-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("mgr-1")).thenReturn(Optional.of(manager));
        when(systemReportService.createNewReport(any())).thenReturn(
                SystemReportResponse.builder()
                        .reportId(2L)
                        .reportName("New Report")
                        .reportStatus(ReportStatus.APPROVED)
                        .reportType(ReportType.MONTHLY_REVENUE)
                        .reportCategory("Finance")
                        .reportData("{\"report\":\"data\"}")
                        .build()
        );

        mockMvc.perform(post("/api/v1/manager/reports")
                        .contentType("application/json")
                        .content("{\"reportName\": \"New Report\", \"reportType\": \"MONTHLY_REVENUE\", \"reportCategory\": \"Finance\", \"reportData\": \"{\\\"report\\\":\\\"data\\\"}\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportId").value(2L));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void createReport_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/manager/reports")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}