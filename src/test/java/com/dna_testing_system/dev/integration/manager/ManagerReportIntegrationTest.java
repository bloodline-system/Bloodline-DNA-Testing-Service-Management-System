package com.dna_testing_system.dev.integration.manager;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.auth.AuthenticationRequestDTO;
import com.dna_testing_system.dev.dto.response.SystemReportResponse;
import com.dna_testing_system.dev.dto.response.auth.AuthTokensResponseDTO;
import com.dna_testing_system.dev.enums.ReportType;
import com.dna_testing_system.dev.service.auth.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dna_testing_system.dev.DevApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Integration tests for ManagerReportController.
 * Uses the test profile and embedded H2 database for integration-level coverage.
 */
@SpringBootTest(classes = DevApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TokenBlacklistServiceTestConfig.class)
@TestPropertySource(properties = {
    "spring.mail.host=localhost",
    "spring.mail.port=1025",
    "spring.mail.username=",
    "spring.mail.password=",
    "spring.websocket.enabled=false",
    "spring.data.redis.repositories.enabled=false",
    "spring.redis.enabled=false"
})
@DisplayName("Manager Report Integration Tests")
class ManagerReportIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String LOGIN_ENDPOINT         = "/api/v1/auth/login";
    private static final String REPORTS_BASE           = "/api/v1/manager/reports";
    private static final String REPORT_BY_ID           = REPORTS_BASE + "/{reportId}";
    private static final String REPORT_STATUS_ENDPOINT = REPORTS_BASE + "/{reportId}/status";

    // Credentials seeded by ApplicationInitConfig at startup
    private static final String MANAGER_USERNAME = "manager";
    private static final String MANAGER_PASSWORD = "manager123";
    private static final String STAFF_USERNAME   = "staff1";
    private static final String STAFF_PASSWORD   = "staff123";

    // ─────────────────────────────────────────────
    // Helper: login and return access token
    // ─────────────────────────────────────────────
    private String loginAndGetAccessToken(String username, String password) throws Exception {
        AuthenticationRequestDTO loginRequest = AuthenticationRequestDTO.builder()
                .username(username)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthTokensResponseDTO> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory()
                        .constructParametricType(ApiResponse.class, AuthTokensResponseDTO.class)
        );
        return response.getData().getAccessToken();
    }

    @BeforeEach
    void setUpMocks() {
        given(tokenBlacklistService.isAccessTokenBlacklisted(anyString())).willReturn(false);
        given(tokenBlacklistService.isRefreshTokenBlacklisted(anyString())).willReturn(false);
    }

    // ─────────────────────────────────────────────
    // Helper: create a report and return its ID
    // ─────────────────────────────────────────────
    private Long createReportAndGetId(String accessToken) throws Exception {
        Map<String, Object> reportRequest = Map.of(
                "reportName",     "Integration Test Report " + System.currentTimeMillis(),
                "reportType",     ReportType.MONTHLY_REVENUE.name(),
                "reportCategory", "DNA_TESTING",
                "reportData",     "{\"summary\": \"test data\"}"
        );

        MvcResult result = mockMvc.perform(post(REPORTS_BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<SystemReportResponse> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory()
                        .constructParametricType(ApiResponse.class, SystemReportResponse.class)
        );
        return response.getData().getReportId();
    }

    // ─────────────────────────────────────────────
    // GET /api/v1/manager/reports
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/manager/reports - Get All Reports")
    class GetAllReportsTests {

        @Test
        @DisplayName("Should return 200 with paginated report list when authenticated as MANAGER")
        void getAllReports_withManagerRole_returns200() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            mockMvc.perform(get(REPORTS_BASE)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.reports").isArray())
                    .andExpect(jsonPath("$.data.currentPage").value(0))
                    .andExpect(jsonPath("$.data.totalReports").isNumber())
                    .andExpect(jsonPath("$.data.totalPages").isNumber())
                    .andExpect(jsonPath("$.data.pageSize").value(20))
                    .andExpect(jsonPath("$.data.stats").exists());
        }

        @Test
        @DisplayName("Should return 401 when no token provided")
        void getAllReports_withoutToken_returns401() throws Exception {
            mockMvc.perform(get(REPORTS_BASE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 403 when authenticated as non-MANAGER role")
        void getAllReports_withStaffRole_returns403() throws Exception {
            String staffToken = loginAndGetAccessToken(STAFF_USERNAME, STAFF_PASSWORD);

            mockMvc.perform(get(REPORTS_BASE)
                            .header("Authorization", "Bearer " + staffToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should support pagination parameters page and size")
        void getAllReports_withPaginationParams_returns200() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            mockMvc.perform(get(REPORTS_BASE)
                            .param("page", "0")
                            .param("size", "5")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentPage").value(0))
                    .andExpect(jsonPath("$.data.pageSize").value(5));
        }

        @Test
        @DisplayName("Should support status filter param")
        void getAllReports_withStatusFilter_returns200() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            mockMvc.perform(get(REPORTS_BASE)
                            .param("status", "GENERATED")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reports").isArray());
        }

        @Test
        @DisplayName("Should support search param")
        void getAllReports_withSearchParam_returns200() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            mockMvc.perform(get(REPORTS_BASE)
                            .param("search", "DNA")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reports").isArray());
        }

        @Test
        @DisplayName("Should support generatedByRole filter param")
        void getAllReports_withRoleFilter_returns200() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            mockMvc.perform(get(REPORTS_BASE)
                            .param("generatedByRole", "MANAGER")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reports").isArray());
        }

        @Test
        @DisplayName("Response should contain report metadata: stats, reportStatuses, reportTypes, allRoles")
        void getAllReports_responseContainsMetadata() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            MvcResult result = mockMvc.perform(get(REPORTS_BASE)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andReturn();

            String body = result.getResponse().getContentAsString();
            assertThat(body).contains("stats");
            assertThat(body).contains("reportStatuses");
            assertThat(body).contains("reportTypes");
            assertThat(body).contains("allRoles");
        }
    }

    // ─────────────────────────────────────────────
    // GET /api/v1/manager/reports/{reportId}
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/manager/reports/{reportId} - Get Report By ID")
    class GetReportByIdTests {

        @Test
        @DisplayName("Should return 200 with report data when report exists")
        void getReportById_withExistingId_returns200() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);
            Long reportId = createReportAndGetId(accessToken);

            mockMvc.perform(get(REPORT_BY_ID, reportId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.reportId").value(reportId))
                    .andExpect(jsonPath("$.data.reportName").isString())
                    .andExpect(jsonPath("$.data.reportType").isString())
                    .andExpect(jsonPath("$.data.reportStatus").isString());
        }

        @Test
        @DisplayName("Should return 404 when report does not exist")
        void getReportById_withNonExistentId_returns404() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            mockMvc.perform(get(REPORT_BY_ID, 999999L)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(404))
                    .andExpect(jsonPath("$.message").value("Report not found"));
        }

        @Test
        @DisplayName("Should return 401 when no token provided")
        void getReportById_withoutToken_returns401() throws Exception {
            mockMvc.perform(get(REPORT_BY_ID, 1L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 403 when authenticated as non-MANAGER role")
        void getReportById_withStaffRole_returns403() throws Exception {
            String staffToken = loginAndGetAccessToken(STAFF_USERNAME, STAFF_PASSWORD);

            mockMvc.perform(get(REPORT_BY_ID, 1L)
                            .header("Authorization", "Bearer " + staffToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────
    // POST /api/v1/manager/reports
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/manager/reports - Create Report")
    class CreateReportTests {

        @Test
        @DisplayName("Should return 201 with created report when request is valid")
        void createReport_withValidRequest_returns201() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            Map<String, Object> reportRequest = Map.of(
                    "reportName",     "Monthly DNA Report " + System.currentTimeMillis(),
                    "reportType",     ReportType.MONTHLY_REVENUE.name(),
                    "reportCategory", "DNA_TESTING",
                    "reportData",     "{\"cases\": 50, \"positive\": 12}"
            );

            mockMvc.perform(post(REPORTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reportRequest))
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(201))
                    .andExpect(jsonPath("$.message").value("Report created successfully"))
                    .andExpect(jsonPath("$.data.reportId").isNumber())
                    .andExpect(jsonPath("$.data.reportName").isString())
                    .andExpect(jsonPath("$.data.reportStatus").value("GENERATED"));
        }

        @Test
        @DisplayName("Should return 401 when no token provided")
        void createReport_withoutToken_returns401() throws Exception {
            Map<String, Object> reportRequest = Map.of(
                    "reportName",     "Test Report",
                    "reportType",     ReportType.MONTHLY_REVENUE.name(),
                    "reportCategory", "DNA_TESTING",
                    "reportData",     "{}"
            );

            mockMvc.perform(post(REPORTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reportRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 403 when authenticated as non-MANAGER role")
        void createReport_withStaffRole_returns403() throws Exception {
            String staffToken = loginAndGetAccessToken(STAFF_USERNAME, STAFF_PASSWORD);

            Map<String, Object> reportRequest = Map.of(
                    "reportName",     "Test Report",
                    "reportType",     ReportType.MONTHLY_REVENUE.name(),
                    "reportCategory", "DNA_TESTING",
                    "reportData",     "{}"
            );

            mockMvc.perform(post(REPORTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reportRequest))
                            .header("Authorization", "Bearer " + staffToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should set generatedByUser from authenticated manager automatically")
        void createReport_setsGeneratedByUserFromToken() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            Map<String, Object> reportRequest = Map.of(
                    "reportName",     "Auto-Author Report",
                    "reportType",     ReportType.STAFF_PRODUCTIVITY.name(),
                    "reportCategory", "STAFF_PERFORMANCE",
                    "reportData",     "{\"data\": \"test\"}"
            );

            MvcResult result = mockMvc.perform(post(REPORTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reportRequest))
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isCreated())
                    .andReturn();

            ApiResponse<SystemReportResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    objectMapper.getTypeFactory()
                            .constructParametricType(ApiResponse.class, SystemReportResponse.class)
            );

            assertThat(response.getData().getGeneratedByUserName()).isNotNull();
        }

        @Test
        @DisplayName("Created report should default to GENERATED status")
        void createReport_defaultStatusIsGenerated() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            Map<String, Object> reportRequest = Map.of(
                    "reportName",     "Status Check Report",
                    "reportType",     ReportType.HEALTH_SYSTEM.name(),
                    "reportCategory", "SYSTEM_SUMMARY",
                    "reportData",     "{}"
            );

            MvcResult result = mockMvc.perform(post(REPORTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reportRequest))
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isCreated())
                    .andReturn();

            ApiResponse<SystemReportResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    objectMapper.getTypeFactory()
                            .constructParametricType(ApiResponse.class, SystemReportResponse.class)
            );

            assertThat(response.getData().getReportStatus().name()).isEqualTo("GENERATED");
        }
    }

    // ─────────────────────────────────────────────
    // PATCH /api/v1/manager/reports/{reportId}/status
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("PATCH /api/v1/manager/reports/{reportId}/status - Update Report Status")
    class UpdateReportStatusTests {

        @Test
        @DisplayName("Should return 200 when status is updated successfully")
        void updateReportStatus_withValidStatus_returns200() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);
            Long reportId = createReportAndGetId(accessToken);

            Map<String, String> body = Map.of("status", "APPROVED");

            mockMvc.perform(patch(REPORT_STATUS_ENDPOINT, reportId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.success").value(true))
                    .andExpect(jsonPath("$.data.message").value("Report status updated successfully to APPROVED"));
        }

        @Test
        @DisplayName("Should return 400 when status field is blank")
        void updateReportStatus_withBlankStatus_returns400() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            Map<String, String> body = Map.of("status", " ");

            mockMvc.perform(patch(REPORT_STATUS_ENDPOINT, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("status không được để trống"));
        }

        @Test
        @DisplayName("Should return 400 when status field is missing from body")
        void updateReportStatus_withMissingStatus_returns400() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            Map<String, String> body = Map.of("otherField", "someValue");

            mockMvc.perform(patch(REPORT_STATUS_ENDPOINT, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when reportId does not exist")
        void updateReportStatus_withNonExistentReportId_returns404() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            Map<String, String> body = Map.of("status", "APPROVED");

            mockMvc.perform(patch(REPORT_STATUS_ENDPOINT, 999999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Report not found"));
        }

        @Test
        @DisplayName("Should return 401 when no token provided")
        void updateReportStatus_withoutToken_returns401() throws Exception {
            Map<String, String> body = Map.of("status", "APPROVED");

            mockMvc.perform(patch(REPORT_STATUS_ENDPOINT, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 403 when authenticated as non-MANAGER role")
        void updateReportStatus_withStaffRole_returns403() throws Exception {
            String staffToken = loginAndGetAccessToken(STAFF_USERNAME, STAFF_PASSWORD);

            Map<String, String> body = Map.of("status", "APPROVED");

            mockMvc.perform(patch(REPORT_STATUS_ENDPOINT, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .header("Authorization", "Bearer " + staffToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should update status to REJECTED successfully")
        void updateReportStatus_toRejected_returns200() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);
            Long reportId = createReportAndGetId(accessToken);

            Map<String, String> body = Map.of("status", "REJECTED");

            mockMvc.perform(patch(REPORT_STATUS_ENDPOINT, reportId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.success").value(true));
        }
    }

    // ─────────────────────────────────────────────
    // End-to-End flow
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("End-to-End Manager Report Flow")
    class EndToEndFlowTests {

        @Test
        @DisplayName("Should complete full report lifecycle: create -> get -> update status -> verify")
        void fullReportLifecycle_succeeds() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            // Step 1: Create report
            Map<String, Object> createRequest = Map.of(
                    "reportName",     "E2E Lifecycle Report",
                    "reportType",     ReportType.MONTHLY_REVENUE.name(),
                    "reportCategory", "DNA_TESTING",
                    "reportData",     "{\"cases\": 100}"
            );

            MvcResult createResult = mockMvc.perform(post(REPORTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest))
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isCreated())
                    .andReturn();

            ApiResponse<SystemReportResponse> createResponse = objectMapper.readValue(
                    createResult.getResponse().getContentAsString(),
                    objectMapper.getTypeFactory()
                            .constructParametricType(ApiResponse.class, SystemReportResponse.class)
            );

            Long reportId = createResponse.getData().getReportId();
            assertThat(reportId).isNotNull().isPositive();
            assertThat(createResponse.getData().getReportStatus().name()).isEqualTo("GENERATED");

            // Step 2: Get report by ID
            mockMvc.perform(get(REPORT_BY_ID, reportId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reportId").value(reportId))
                    .andExpect(jsonPath("$.data.reportStatus").value("GENERATED"));

            // Step 3: Approve the report
            Map<String, String> approveBody = Map.of("status", "APPROVED");

            mockMvc.perform(patch(REPORT_STATUS_ENDPOINT, reportId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approveBody))
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.success").value(true));

            // Step 4: Verify the updated status by fetching again
            mockMvc.perform(get(REPORT_BY_ID, reportId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reportId").value(reportId))
                    .andExpect(jsonPath("$.data.reportStatus").value("APPROVED"));
        }

        @Test
        @DisplayName("Unauthenticated user is blocked from all report endpoints")
        void unauthenticatedUser_isBlockedFromAllEndpoints() throws Exception {
            mockMvc.perform(get(REPORTS_BASE)).andExpect(status().isUnauthorized());
            mockMvc.perform(get(REPORT_BY_ID, 1L)).andExpect(status().isUnauthorized());
            mockMvc.perform(post(REPORTS_BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")).andExpect(status().isUnauthorized());
            mockMvc.perform(patch(REPORT_STATUS_ENDPOINT, 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"APPROVED\"}")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Report appears in list after creation")
        void createReport_thenAppearsInList() throws Exception {
            String accessToken = loginAndGetAccessToken(MANAGER_USERNAME, MANAGER_PASSWORD);

            String uniqueName = "Unique Report " + System.currentTimeMillis();
            Map<String, Object> createRequest = Map.of(
                    "reportName",     uniqueName,
                    "reportType",     ReportType.STAFF_PRODUCTIVITY.name(),
                    "reportCategory", "TEST_CATEGORY",
                    "reportData",     "{}"
            );

            // Create the report
            mockMvc.perform(post(REPORTS_BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest))
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isCreated());

            // Search for it in the list
            MvcResult listResult = mockMvc.perform(get(REPORTS_BASE)
                            .param("search", uniqueName)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andReturn();

            String body = listResult.getResponse().getContentAsString();
            assertThat(body).contains(uniqueName);
        }
    }
}