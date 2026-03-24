package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.NewReportRequest;
import com.dna_testing_system.dev.dto.request.UpdatingReportRequest;
import com.dna_testing_system.dev.dto.request.manager.UpdateReportStatusRequest;
import com.dna_testing_system.dev.dto.response.SystemReportResponse;
import com.dna_testing_system.dev.dto.response.manager.ListReportsResponse;
import com.dna_testing_system.dev.dto.response.manager.ReportStatsResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.ReportStatus;
import com.dna_testing_system.dev.enums.ReportType;
import com.dna_testing_system.dev.enums.RoleType;
import com.dna_testing_system.dev.exception.EntityNotFoundException;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.SystemReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager/reports")
public class ManagerReportController {

    private final SystemReportService systemReportService;
    private final UserRepository userRepository;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ListReportsResponse> listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "all") String generatedByRole,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        ReportQueryParams params = normalizeParams(page, size, status, generatedByRole, search, sortBy, sortDir);

        List<SystemReportResponse> allReports = systemReportService.getAllSystemReports();
        List<SystemReportResponse> filteredReports = filterAndSortReports(allReports, params);

        ReportStatsResponse reportStats = calculateReportStatistics(allReports);

        List<SystemReportResponse> pageReports = paginateReports(filteredReports, params.page(), params.size());

        ListReportsResponse response = ListReportsResponse.builder()
                .reports(pageReports)
                .reportStats(reportStats)
                .pagination(Map.of(
                        "currentPage", params.page(),
                        "pageSize", params.size(),
                        "totalPages", calculateTotalPages(filteredReports.size(), params.size()),
                        "totalReports", filteredReports.size()))
                .filters(Map.of(
                        "status", params.status(),
                        "generatedByRole", params.generatedByRole(),
                        "search", params.search(),
                        "sortBy", params.sortBy(),
                        "sortDir", params.sortDir()))
                .allRoles(RoleType.values())
                .reportStatuses(ReportStatus.values())
                .reportTypes(ReportType.values())
                .build();

        return ApiResponse.success(HttpStatus.OK.value(), "Reports retrieved successfully", response);
    }

    private record ReportQueryParams(int page, int size, String status, String generatedByRole, String search, String sortBy, String sortDir) {}

    private ReportQueryParams normalizeParams(int page, int size, String status, String generatedByRole, String search, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        String safeStatus = status == null ? "all" : status.trim();
        String safeRole = generatedByRole == null ? "all" : generatedByRole.trim();
        String safeSearch = search == null ? "" : search.trim().toLowerCase();
        String safeSortBy = sortBy == null ? "createdAt" : sortBy.trim();
        String safeSortDir = sortDir == null ? "desc" : sortDir.trim().toLowerCase();
        return new ReportQueryParams(safePage, safeSize, safeStatus, safeRole, safeSearch, safeSortBy, safeSortDir);
    }

    private List<SystemReportResponse> filterAndSortReports(List<SystemReportResponse> allReports, ReportQueryParams params) {
        return allReports.stream()
                .filter(report -> "all".equalsIgnoreCase(params.status()) || report.getReportStatus() != null && report.getReportStatus().name().equalsIgnoreCase(params.status()))
                .filter(report -> "all".equalsIgnoreCase(params.generatedByRole()) || safeString(report.getGeneratedByUserRole()).equalsIgnoreCase(params.generatedByRole()))
                .filter(report -> params.search().isEmpty() ||
                        safeString(report.getReportName()).toLowerCase().contains(params.search()) ||
                        safeString(report.getReportCategory()).toLowerCase().contains(params.search()) ||
                        safeString(report.getGeneratedByUserName()).toLowerCase().contains(params.search()))
                .sorted((r1, r2) -> {
                    int result;
                    switch (params.sortBy()) {
                        case "reportName":
                            result = compareSafe(r1.getReportName(), r2.getReportName());
                            break;
                        case "reportCategory":
                            result = compareSafe(r1.getReportCategory(), r2.getReportCategory());
                            break;
                        case "generatedByUserRole":
                            result = compareSafe(r1.getGeneratedByUserRole(), r2.getGeneratedByUserRole());
                            break;
                        case "reportStatus":
                            result = compareSafe(reportStatusName(r1), reportStatusName(r2));
                            break;
                        case "reportType":
                            result = compareSafe(reportTypeName(r1), reportTypeName(r2));
                            break;
                        case "createdAt":
                            result = compareDates(r1.getCreatedAt(), r2.getCreatedAt());
                            break;
                        default:
                            result = 0;
                    }
                    return "desc".equalsIgnoreCase(params.sortDir()) ? -result : result;
                })
                .toList();
    }

    private int calculateTotalPages(int totalReports, int size) {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalReports / size);
    }

    private List<SystemReportResponse> paginateReports(List<SystemReportResponse> reports, int page, int size) {
        int total = reports.size();
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);
        return reports.subList(start, end);
    }

    private <T extends Comparable<? super T>> int compareDates(T d1, T d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return -1;
        if (d2 == null) return 1;
        return d1.compareTo(d2);
    }

    @GetMapping("/{reportId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<SystemReportResponse> getReportDetails(@PathVariable Long reportId) {
        SystemReportResponse report = systemReportService.getSystemReportByReportId(reportId);
        return ApiResponse.success(HttpStatus.OK.value(), "Report details retrieved", report);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createReport(@Valid @RequestBody NewReportRequest reportRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : null;
        if (currentUsername == null) {
            throw new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS);
        }
        var existingUser = userRepository.findByUsername(currentUsername);
        String currentUserId = existingUser
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS)).getId();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        reportRequest.setGeneratedByUser(currentUser);
        systemReportService.createNewReport(reportRequest);

        return ApiResponse.success(HttpStatus.CREATED.value(), "Report created successfully", null);
    }

    @PatchMapping("/{reportId}/status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updateReportStatus(@PathVariable Long reportId,
            @Valid @RequestBody UpdateReportStatusRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : null;
        if (currentUsername == null) {
            throw new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS);
        }
        var existingUser = userRepository.findByUsername(currentUsername);
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
                .newReportStatus(request.getStatus().toUpperCase())
                .build();

        systemReportService.updateExistReport(updateRequest, reportId);

        return ApiResponse.success(HttpStatus.OK.value(), "Report status updated successfully to " + request.getStatus(), null);
    }

    private ReportStatsResponse calculateReportStatistics(List<SystemReportResponse> reports) {
        long totalReports = reports.size();
        long generatedReports = reports.stream().filter(r -> r.getReportStatus() == ReportStatus.GENERATED).count();
        long approvedReports = reports.stream().filter(r -> r.getReportStatus() == ReportStatus.APPROVED).count();
        long rejectedReports = reports.stream().filter(r -> r.getReportStatus() == ReportStatus.REJECTED).count();

        // Count by type
        Map<ReportType, Long> reportsByType = new EnumMap<>(ReportType.class);
        for (ReportType type : ReportType.values()) {
            long count = reports.stream().filter(r -> r.getReportType() == type).count();
            reportsByType.put(type, count);
        }

        return ReportStatsResponse.builder()
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
}

