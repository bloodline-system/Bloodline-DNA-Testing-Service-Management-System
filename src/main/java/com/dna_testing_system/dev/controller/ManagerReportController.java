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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                            result = 0;
                            break;
                    }
                    return "desc".equals(sortDir) ? -result : result;
                })
                .collect(Collectors.toList());

        // Calculate report statistics
        ReportStatsResponse reportStats = calculateReportStatistics(allReports);

        // Pagination
        int totalReports = filteredReports.size();
        int startItem = page * size;
        int endItem = Math.min(startItem + size, totalReports);
        List<SystemReportResponse> pageReports = filteredReports.subList(startItem, endItem);
        int totalPages = (int) Math.ceil((double) totalReports / size);

        ListReportsResponse response = ListReportsResponse.builder()
                .reports(pageReports)
                .reportStats(reportStats)
                .pagination(Map.of(
                        "currentPage", page,
                        "pageSize", size,
                        "totalPages", totalPages,
                        "totalReports", totalReports))
                .filters(Map.of(
                        "status", status,
                        "generatedByRole", generatedByRole,
                        "search", search,
                        "sortBy", sortBy,
                        "sortDir", sortDir))
                .allRoles(RoleType.values())
                .reportStatuses(ReportStatus.values())
                .reportTypes(ReportType.values())
                .build();

        return ApiResponse.success(HttpStatus.OK.value(), "Reports retrieved successfully", response);
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
        var existingUser = userRepository.findByUsername(auth.getName());
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
        Map<ReportType, Long> reportsByType = new HashMap<>();
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
}
