package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.NewReportRequest;
import com.dna_testing_system.dev.dto.response.SystemReportResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.ReportStatus;
import com.dna_testing_system.dev.enums.ReportType;
import com.dna_testing_system.dev.repository.RoleRepository;
import com.dna_testing_system.dev.repository.UserProfileRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.repository.UserRoleRepository;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.NotificationService;
import com.dna_testing_system.dev.service.SystemReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
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
class ManagerReportControllerTest {

    @Autowired
    MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    SystemReportService systemReportService;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    NotificationService notificationService;

    @MockitoBean
    RoleRepository roleRepository;

    @MockitoBean
    UserRoleRepository userRoleRepository;

    @MockitoBean
    UserProfileRepository userProfileRepository;

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .exceptionHandling(exc -> exc.authenticationEntryPoint((request, response, authException) -> response.setStatus(401)));
            return http.build();
        }
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getAllReports_withManager_returns200() throws Exception {
        SystemReportResponse report = SystemReportResponse.builder()
                .reportId(1L)
                .reportName("Daily Summary")
                .reportCategory("Finance")
                .reportType(ReportType.DAILY_ORDERS)
                .generatedByUserName("Manager User")
                .generatedByUserRole("MANAGER")
                .reportStatus(ReportStatus.GENERATED)
                .build();

        when(systemReportService.getAllSystemReports()).thenReturn(List.of(report));

        mockMvc.perform(get("/api/v1/manager/reports")
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "all")
                        .param("generatedByRole", "all")
                        .param("sortBy", "createdAt")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReports").value(1))
                .andExpect(jsonPath("$.data.reports[0].reportId").value(1));

        verify(systemReportService).getAllSystemReports();
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getReportById_withManager_returns200() throws Exception {
        SystemReportResponse report = SystemReportResponse.builder()
                .reportId(7L)
                .reportName("Monthly Sales")
                .reportType(ReportType.MONTHLY_REVENUE)
                .reportCategory("Revenue")
                .reportStatus(ReportStatus.APPROVED)
                .generatedByUserName("Manager User")
                .build();

        when(systemReportService.getSystemReportByReportId(7L)).thenReturn(report);

        mockMvc.perform(get("/api/v1/manager/reports/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(7))
                .andExpect(jsonPath("$.data.reportName").value("Monthly Sales"));

        verify(systemReportService).getSystemReportByReportId(7L);
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void createReport_withManager_returns201() throws Exception {
        User manager = User.builder().id("user-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(manager));

        NewReportRequest request = NewReportRequest.builder()
                .reportName("Weekly Performance")
                .reportType(ReportType.STAFF_PRODUCTIVITY)
                .reportCategory("Productivity")
                .reportData("data test")
                .build();

        SystemReportResponse created = SystemReportResponse.builder()
                .reportId(33L)
                .reportName("Weekly Performance")
                .reportType(ReportType.STAFF_PRODUCTIVITY)
                .reportCategory("Productivity")
                .generatedByUserName("Manager User")
                .reportStatus(ReportStatus.GENERATED)
                .build();

        when(systemReportService.createNewReport(any(NewReportRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/manager/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reportId").value(33))
                .andExpect(jsonPath("$.data.reportStatus").value("GENERATED"));

        verify(systemReportService).createNewReport(any(NewReportRequest.class));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updateReportStatus_withManager_returns200() throws Exception {
        User manager = User.builder().id("user-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(manager));

        SystemReportResponse existing = SystemReportResponse.builder()
                .reportId(44L)
                .reportName("Quality Report")
                .reportType(ReportType.QUALITY_METRICS)
                .reportCategory("Quality")
                .reportData("xyz")
                .generatedByUserName("Manager User")
                .generatedByUserRole("MANAGER")
                .reportStatus(ReportStatus.GENERATED)
                .build();

        when(systemReportService.getSystemReportByReportId(44L)).thenReturn(existing);
        doNothing().when(systemReportService).updateExistReport(any(), eq(44L));

        mockMvc.perform(patch("/api/v1/manager/reports/44/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));

        verify(systemReportService).updateExistReport(any(), eq(44L));
    }

    @Test
    void getAllReports_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/manager/reports"))
                .andExpect(status().isUnauthorized());
    }
}
