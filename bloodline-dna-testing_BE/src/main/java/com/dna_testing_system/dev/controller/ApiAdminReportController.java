package com.dna_testing_system.dev.controller;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApiAdminReportController {

    SystemReportService systemReportService;
    UserRepository userRepository;

    // GET ALL + filter + pagination + stats
    @GetMapping
    public ResponseEntity<?> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "all") String generatedByRole,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            List<SystemReportResponse> allReports = systemReportService.getAllSystemReports();

            List<SystemReportResponse> filtered = allReports.stream()
                    .filter(r -> "all".equals(status) || r.getReportStatus().name().equalsIgnoreCase(status))
                    .filter(r -> "all".equals(generatedByRole) ||
                            (r.getGeneratedByUserRole() != null && r.getGeneratedByUserRole().equalsIgnoreCase(generatedByRole)))
                    .filter(r -> search.isEmpty() ||
                            r.getReportName().toLowerCase().contains(search.toLowerCase()) ||
                            r.getReportCategory().toLowerCase().contains(search.toLowerCase()) ||
                            r.getGeneratedByUserName().toLowerCase().contains(search.toLowerCase()))
                    .sorted((r1, r2) -> {
                        int result = switch (sortBy) {
                            case "reportName" -> r1.getReportName().compareTo(r2.getReportName());
                            case "reportCategory" -> r1.getReportCategory().compareTo(r2.getReportCategory());
                            case "generatedByUserRole" -> (r1.getGeneratedByUserRole() != null ? r1.getGeneratedByUserRole() : "")
                                    .compareTo(r2.getGeneratedByUserRole() != null ? r2.getGeneratedByUserRole() : "");
                            case "reportStatus" -> r1.getReportStatus().name().compareTo(r2.getReportStatus().name());
                            case "reportType" -> r1.getReportType().name().compareTo(r2.getReportType().name());
                            default -> 0;
                        };
                        return "desc".equals(sortDir) ? -result : result;
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

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error loading reports: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to load reports data");
        }
    }

    // GET BY ID
    @GetMapping("/{reportId}")
    public ResponseEntity<?> getReportById(@PathVariable Long reportId) {
        try {
            SystemReportResponse report = systemReportService.getSystemReportByReportId(reportId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error loading report details: ", e);
            return ResponseEntity.notFound().build();
        }
    }

    // CREATE
    @PostMapping
    public ResponseEntity<?> createReport(@RequestBody NewReportRequest reportRequest) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userRepository.findByUsername(auth.getName())
                    .map(u -> userRepository.findById(u.getId())
                            .orElseThrow(() -> new RuntimeException("User not found")))
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));

            reportRequest.setGeneratedByUser(currentUser);
            systemReportService.createNewReport(reportRequest);

            return ResponseEntity.status(HttpStatus.CREATED).body("Report created successfully");

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        } catch (Exception e) {
            log.error("Error creating report: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create report: " + e.getMessage());
        }
    }

    // UPDATE STATUS
    @PatchMapping("/{reportId}/status")
    public ResponseEntity<?> updateReportStatus(
            @PathVariable Long reportId,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body("status không được để trống");
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
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

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Report status updated successfully to " + status
            ));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        } catch (Exception e) {
            log.error("Error updating report status: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Failed to update report status: " + e.getMessage()
            ));
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
