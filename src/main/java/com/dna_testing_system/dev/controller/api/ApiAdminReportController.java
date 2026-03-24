package com.dna_testing_system.dev.controller.api;

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
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/admin/reports")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class ApiAdminReportController {

    SystemReportService systemReportService;
    UserRepository userRepository;

    // GET ALL + filter + pagination + stats
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "all") String generatedByRole,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {

        try {
            if (page < 0) {
                page = 0;
            }
            if (size <= 0) {
                size = 20;
            }

            String safeStatus = status == null ? "all" : status.trim();
            String safeGeneratedByRole = generatedByRole == null ? "all" : generatedByRole.trim();
            String safeSearch = search == null ? "" : search.trim().toLowerCase();
            String safeSortBy = sortBy == null ? "createdAt" : sortBy;
            String safeSortDir = sortDir == null ? "desc" : sortDir.toLowerCase();

            List<SystemReportResponse> allReports = systemReportService.getAllSystemReports();

            List<SystemReportResponse> filtered = allReports.stream()
                    .filter(r -> "all".equalsIgnoreCase(safeStatus) ||
                            (r.getReportStatus() != null && r.getReportStatus().name().equalsIgnoreCase(safeStatus)))
                    .filter(r -> "all".equalsIgnoreCase(safeGeneratedByRole) ||
                            safeString(r.getGeneratedByUserRole()).equalsIgnoreCase(safeGeneratedByRole))
                    .filter(r -> safeSearch.isEmpty() ||
                            safeString(r.getReportName()).toLowerCase().contains(safeSearch) ||
                            safeString(r.getReportCategory()).toLowerCase().contains(safeSearch) ||
                            safeString(r.getGeneratedByUserName()).toLowerCase().contains(safeSearch))
                    .sorted((r1, r2) -> {
                        int result;
                        switch (safeSortBy) {
                            case "reportName" -> result = compareSafe(r1.getReportName(), r2.getReportName());
                            case "reportCategory" -> result = compareSafe(r1.getReportCategory(), r2.getReportCategory());
                            case "generatedByUserRole" -> result = compareSafe(r1.getGeneratedByUserRole(), r2.getGeneratedByUserRole());
                            case "reportStatus" -> result = compareSafe(reportStatusName(r1), reportStatusName(r2));
                            case "reportType" -> result = compareSafe(reportTypeName(r1), reportTypeName(r2));
                            default -> result = 0;
                        }
                        return "desc".equalsIgnoreCase(safeSortDir) ? -result : result;
                    })
                    .collect(Collectors.toList());

            // Pagination
            int start = Math.min(page * size, filtered.size());
            int end = Math.min(start + size, filtered.size());
            List<SystemReportResponse> pageContent = filtered.subList(start, end);
            Page<SystemReportResponse> reportPage = new PageImpl<>(pageContent, PageRequest.of(page, size), filtered.size());

            ReportStats stats = calculateReportStatistics(allReports);

            Map<String, Object> response = Map.of(
                    "reports", pageContent,
                    "currentPage", page,
                    "totalPages", reportPage.getTotalPages(),
                    "totalReports", filtered.size(),
                    "pageSize", size,
                    "stats", stats,
                    "reportStatuses", ReportStatus.values(),
                    "reportTypes", ReportType.values(),
                    "allRoles", RoleType.values()
            );

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Report list loaded", response));

        } catch (Exception e) {
            log.error("Error loading reports: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to load reports data", request.getRequestURI()));
        }
    }

    // GET BY ID
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<SystemReportResponse>> getReportById(@PathVariable Long reportId,
                                                                           HttpServletRequest request) {
        try {
            SystemReportResponse report = systemReportService.getSystemReportByReportId(reportId);
            if (report == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Report not found", request.getRequestURI()));
            }
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Report loaded", report));
        } catch (Exception e) {
            log.error("Error loading report details: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to load report details", request.getRequestURI()));
        }
    }

    // CREATE
    @PostMapping
    public ResponseEntity<ApiResponse<SystemReportResponse>> createReport(@RequestBody NewReportRequest reportRequest,
                                                                          HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                throw new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS);
            }
            User currentUser = userRepository.findByUsername(auth.getName())
                    .map(u -> userRepository.findById(u.getId())
                            .orElseThrow(() -> new RuntimeException("User not found")))
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));

            reportRequest.setGeneratedByUser(currentUser);
            SystemReportResponse created = systemReportService.createNewReport(reportRequest);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(HttpStatus.CREATED.value(), "Report created successfully", created));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not found", request.getRequestURI()));
        } catch (Exception e) {
            log.error("Error creating report: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to create report: " + e.getMessage(), request.getRequestURI()));
        }
    }

    // UPDATE STATUS
    @PatchMapping("/{reportId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateReportStatus(
            @PathVariable Long reportId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "status không được để trống", request.getRequestURI()));
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                throw new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS);
            }
            String currentUserId = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS))
                    .getId();

            SystemReportResponse currentReport = systemReportService.getSystemReportByReportId(reportId);

            UpdatingReportRequest updateRequest = UpdatingReportRequest.builder()
                    .reportName(currentReport.getReportName())
                    .reportType(currentReport.getReportType())
                    .reportCategory(currentReport.getReportCategory())
                    .generatedByUserId(currentUserId)
                    .reportData(currentReport.getReportData())
                    .newReportStatus(status.toUpperCase())
                    .build();

            systemReportService.updateExistReport(updateRequest, reportId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Report status updated successfully to " + status
            );

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Report status updated", response));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not found", request.getRequestURI()));
        } catch (Exception e) {
            log.error("Error updating report status: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to update report status: " + e.getMessage(), request.getRequestURI()));
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

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private int compareSafe(String first, String second) {
        return safeString(first).compareTo(safeString(second));
    }

    private String reportStatusName(SystemReportResponse report) {
        return report != null && report.getReportStatus() != null ? report.getReportStatus().name() : "";
    }

    private String reportTypeName(SystemReportResponse report) {
        return report != null && report.getReportType() != null ? report.getReportType().name() : "";
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
