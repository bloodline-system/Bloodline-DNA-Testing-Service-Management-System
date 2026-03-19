package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.NewReportRequest;
import com.dna_testing_system.dev.dto.request.UpdatingReportRequest;
import com.dna_testing_system.dev.dto.response.SystemReportResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.ReportStatus;
import com.dna_testing_system.dev.enums.ReportType;
import com.dna_testing_system.dev.enums.RoleType;
import com.dna_testing_system.dev.exception.EntityNotFoundException;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.SystemReportService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager/reports")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManagerReportController {

    SystemReportService systemReportService;
    UserRepository userRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "all") String generatedByRole,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Get all reports
            List<SystemReportResponse> allReports = systemReportService.getAllSystemReports();

            // Filter reports
            List<SystemReportResponse> filteredReports = allReports.stream()
                    .filter(report -> {
                        if (!"all".equals(status)) {
                            return report.getReportStatus().name().equalsIgnoreCase(status);
                        }
                        return true;
                    })
                    .filter(report -> {
                        if (!"all".equals(generatedByRole)) {
                            return report.getGeneratedByUserRole() != null &&
                                    report.getGeneratedByUserRole().equalsIgnoreCase(generatedByRole);
                        }
                        return true;
                    })
                    .filter(report -> {
                        if (!search.isEmpty()) {
                            return report.getReportName().toLowerCase().contains(search.toLowerCase()) ||
                                    report.getReportCategory().toLowerCase().contains(search.toLowerCase()) ||
                                    report.getGeneratedByUserName().toLowerCase().contains(search.toLowerCase());
                        }
                        return true;
                    })
                    .sorted((r1, r2) -> {
                        int result = 0;
                        switch (sortBy) {
                            case "reportName":
                                result = r1.getReportName().compareTo(r2.getReportName());
                                break;
                            case "reportCategory":
                                result = r1.getReportCategory().compareTo(r2.getReportCategory());
                                break;
                            case "generatedByUserRole":
                                result = (r1.getGeneratedByUserRole() != null ? r1.getGeneratedByUserRole() : "")
                                        .compareTo(
                                                r2.getGeneratedByUserRole() != null ? r2.getGeneratedByUserRole() : "");
                                break;
                            case "reportStatus":
                                result = r1.getReportStatus().name().compareTo(r2.getReportStatus().name());
                                break;
                            case "reportType":
                                result = r1.getReportType().name().compareTo(r2.getReportType().name());
                                break;
                            case "createdAt":
                            default:
                                result = 0; // SystemReportResponse doesn't have createdAt, would need to be added
                                break;
                        }
                        return "desc".equals(sortDir) ? -result : result;
                    })
                    .collect(Collectors.toList());

            // Calculate report statistics
            ReportStats reportStats = calculateReportStatistics(allReports);

            // Pagination
            int totalReports = filteredReports.size();
            int startItem = page * size;
            int endItem = Math.min(startItem + size, totalReports);
            List<SystemReportResponse> pageReports = filteredReports.subList(startItem, endItem);
            int totalPages = (int) Math.ceil((double) totalReports / size);

            response.put("reports", pageReports);
            response.put("reportStats", reportStats);
            response.put("pagination", Map.of(
                    "currentPage", page,
                    "pageSize", size,
                    "totalPages", totalPages,
                    "totalReports", totalReports));
            response.put("filters", Map.of(
                    "status", status,
                    "generatedByRole", generatedByRole,
                    "search", search,
                    "sortBy", sortBy,
                    "sortDir", sortDir));

            // Additional data for UI dropdowns
            response.put("allRoles", RoleType.values());
            response.put("reportStatuses", ReportStatus.values());
            response.put("reportTypes", ReportType.values());

            return ApiResponse.success(200, "Reports retrieved successfully", response);
        } catch (Exception e) {
            log.error("Error listing reports: ", e);
            return ApiResponse.error(500, "Unable to load reports data", "/api/v1/manager/reports");
        }
    }

    @GetMapping("/{reportId}")
    public ApiResponse<SystemReportResponse> getReportDetails(@PathVariable Long reportId) {
        try {
            SystemReportResponse report = systemReportService.getSystemReportByReportId(reportId);
            return ApiResponse.success(200, "Report details retrieved", report);
        } catch (Exception e) {
            log.error("Error loading report details: ", e);
            return ApiResponse.error(500, "Unable to load report details", "/api/v1/manager/reports/" + reportId);
        }
    }

    @PostMapping
    public ApiResponse<Void> createReport(@RequestBody NewReportRequest reportRequest) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            var existingUser = userRepository.findByUsername(auth.getName());
            String currentUserId = existingUser
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS)).getId();

            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            reportRequest.setGeneratedByUser(currentUser);
            systemReportService.createNewReport(reportRequest);

            return ApiResponse.success(201, "Report created successfully", null);
        } catch (Exception e) {
            log.error("Error creating report: ", e);
            return ApiResponse.error(500, "Failed to create report", "/api/v1/manager/reports");
        }
    }

    @PatchMapping("/{reportId}/status")
    public ApiResponse<Void> updateReportStatus(@PathVariable Long reportId,
            @RequestParam String status,
            @RequestParam(required = false) String notes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            var existingUser = userRepository.findByUsername(auth.getName());
            String currentUserId = existingUser
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS)).getId();

            // Get current report
            SystemReportResponse currentReport = systemReportService.getSystemReportByReportId(reportId);

            // Create update request
            UpdatingReportRequest updateRequest = UpdatingReportRequest.builder()
                    .reportName(currentReport.getReportName())
                    .reportType(currentReport.getReportType())
                    .reportCategory(currentReport.getReportCategory())
                    .generatedByUserId(currentUserId)
                    .reportData(currentReport.getReportData())
                    .newReportStatus(status.toUpperCase())
                    .build();

            systemReportService.updateExistReport(updateRequest, reportId);

            return ApiResponse.success(200, "Report status updated successfully to " + status, null);
        } catch (Exception e) {
            log.error("Error updating report status: ", e);
            return ApiResponse.error(500, "Failed to update report status",
                    "/api/v1/manager/reports/" + reportId + "/status");
        }
    }

    private ReportStats calculateReportStatistics(List<SystemReportResponse> reports) {
        long totalReports = reports.size();
        long generatedReports = reports.stream().filter(r -> r.getReportStatus() == ReportStatus.GENERATED).count();
        long approvedReports = reports.stream().filter(r -> r.getReportStatus() == ReportStatus.APPROVED).count();
        long rejectedReports = reports.stream().filter(r -> r.getReportStatus() == ReportStatus.REJECTED).count();

        // Count by type
        Map<ReportType, Long> reportsByType = new HashMap<>();
        for (ReportType type : ReportType.values()) {
            long count = reports.stream().filter(r -> r.getReportType() == type).count();
            reportsByType.put(type, count);
        }

        return ReportStats.builder()
                .totalReports(totalReports)
                .generatedReports(generatedReports)
                .approvedReports(approvedReports)
                .rejectedReports(rejectedReports)
                .reportsByType(reportsByType)
                .build();
    }

    // Inner class for statistics
    @lombok.Data
    @lombok.Builder
    public static class ReportStats {
        private long totalReports;
        private long generatedReports;
        private long approvedReports;
        private long rejectedReports;
        private Map<ReportType, Long> reportsByType;
    }
}
