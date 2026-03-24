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
import com.dna_testing_system.dev.enums.ServiceOrderStatus;
import com.dna_testing_system.dev.exception.EntityNotFoundException;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import com.dna_testing_system.dev.service.SystemReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager/dna-statistics")
public class ManagerStatisticsController {

    private final SystemReportService systemReportService;
    private final UserRepository userRepository;
    private final OrderTaskManagementService orderTaskManagementService;

    @GetMapping("/overview")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> overview() {
        var orders = orderTaskManagementService.getServiceOrders();
        long totalOrders = orders.size();
        long pendingOrders = orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.PENDING).count();
        long inProgressOrders = orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.IN_PROGRESS).count();
        long completedOrders = orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.COMPLETED).count();
        long cancelledOrders = orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.CANCELLED).count();

        double completionRate = totalOrders == 0 ? 0.0 : (double) completedOrders / totalOrders * 100;
        double cancellationRate = totalOrders == 0 ? 0.0 : (double) cancelledOrders / totalOrders * 100;

        long currentMonthOrders = orders.stream().filter(o -> o.getCreatedAt() != null && o.getCreatedAt().getMonth() == LocalDate.now().getMonth()).count();
        long prevMonthOrders = orders.stream().filter(o -> o.getCreatedAt() != null && o.getCreatedAt().getMonth() == LocalDate.now().minusMonths(1).getMonth()).count();
        double monthGrowthRate = prevMonthOrders == 0 ? 100.0 : (double) (currentMonthOrders - prevMonthOrders) / prevMonthOrders * 100;

        Map<String, Object> statusBreakdown = new HashMap<>();
        statusBreakdown.put("pending", pendingOrders);
        statusBreakdown.put("inProgress", inProgressOrders);
        statusBreakdown.put("completed", completedOrders);
        statusBreakdown.put("cancelled", cancelledOrders);

        Map<String, Object> revenue = new HashMap<>();
        double totalRevenue = orders.stream()
                .filter(o -> o.getOrderStatus() == ServiceOrderStatus.COMPLETED)
                .mapToDouble(o -> o.getFinalAmount() != null ? o.getFinalAmount().doubleValue() : 0.0)
                .sum();
        revenue.put("totalRevenue", totalRevenue);
        revenue.put("formattedTotal", String.format("%,.0f VNĐ", totalRevenue));

        var payload = Map.<String, Object>of(
                "totalOrders", totalOrders,
                "statusBreakdown", statusBreakdown,
                "completionRate", String.format("%.2f%%", completionRate),
                "cancellationRate", String.format("%.2f%%", cancellationRate),
                "currentMonthOrders", currentMonthOrders,
                "prevMonthOrders", prevMonthOrders,
                "monthGrowthRate", String.format("%.2f%%", monthGrowthRate),
                "pendingOrders", pendingOrders,
                "inProgressOrders", inProgressOrders,
                "revenue", revenue
        );

        return ApiResponse.success(HttpStatus.OK.value(), "Overview statistics", payload);
    }

    @GetMapping("/by-period")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> byPeriod(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(defaultValue = "day") String groupBy,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDate start = startDate != null ? LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE) : LocalDate.now();

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("startDate must be before or equal endDate");
        }

        String normalizedGroupBy = groupBy == null ? "day" : groupBy.trim().toLowerCase();
        if (!List.of("day", "week", "month", "year").contains(normalizedGroupBy)) {
            throw new IllegalArgumentException("groupBy must be one of day, week, month, year");
        }

        var orders = orderTaskManagementService.getServiceOrders().stream()
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> {
                    LocalDate d = o.getCreatedAt().toLocalDate();
                    return !d.isBefore(start) && !d.isAfter(end);
                })
                .toList();

        var aggregated = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> formatPeriodKey(o.getCreatedAt().toLocalDate(), normalizedGroupBy),
                        Collectors.counting()));

        Map<String, Number> chartData = new LinkedHashMap<>();

        LocalDate cursor;
        switch (normalizedGroupBy) {
            case "week" -> cursor = start.with(WeekFields.ISO.dayOfWeek(), 1);
            case "month" -> cursor = start.withDayOfMonth(1);
            case "year" -> cursor = start.withDayOfYear(1);
            default -> cursor = start;
        }

        while (!cursor.isAfter(end)) {
            String key = formatPeriodKey(cursor, normalizedGroupBy);
            chartData.put(key, aggregated.getOrDefault(key, 0L));
            cursor = advanceCursor(cursor, normalizedGroupBy);
        }

        long totalInPeriod = orders.size();
        long completedInPeriod = orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.COMPLETED).count();
        long cancelledInPeriod = orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.CANCELLED).count();
        double completionRate = totalInPeriod == 0 ? 0.0 : (double) completedInPeriod / totalInPeriod * 100;

        var payload = Map.<String, Object>of(
                "period", period,
                "groupBy", normalizedGroupBy,
                "startDate", start.format(DateTimeFormatter.ISO_DATE),
                "endDate", end.format(DateTimeFormatter.ISO_DATE),
                "chartData", chartData,
                "totalInPeriod", totalInPeriod,
                "completedInPeriod", completedInPeriod,
                "cancelledInPeriod", cancelledInPeriod,
                "completionRate", String.format("%.2f%%", completionRate)
        );

        return ApiResponse.success(HttpStatus.OK.value(), "By period statistics", payload);
    }

    private String formatPeriodKey(LocalDate date, String groupBy) {
        return switch (groupBy) {
            case "week" -> {
                var weekFields = WeekFields.ISO;
                int weekYear = date.get(weekFields.weekBasedYear());
                int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
                yield String.format("%d-W%02d", weekYear, weekNumber);
            }
            case "month" -> date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            case "year" -> date.format(DateTimeFormatter.ofPattern("yyyy"));
            default -> date.format(DateTimeFormatter.ISO_DATE);
        };
    }

    private LocalDate advanceCursor(LocalDate cursor, String groupBy) {
        return switch (groupBy) {
            case "week" -> cursor.plusWeeks(1);
            case "month" -> cursor.plusMonths(1);
            case "year" -> cursor.plusYears(1);
            default -> cursor.plusDays(1);
        };
    }

    @GetMapping("/by-status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> byStatus() {
        var orders = orderTaskManagementService.getServiceOrders();
        long total = orders.size();

        List<Map<String, Object>> breakdown = List.of(
                Map.of("status", "PENDING", "count", orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.PENDING).count(), "percentage", total == 0 ? "0%" : String.format("%.2f%%", orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.PENDING).count() * 100.0 / total)),
                Map.of("status", "CONFIRMED", "count", orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.CONFIRMED).count(), "percentage", total == 0 ? "0%" : String.format("%.2f%%", orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.CONFIRMED).count() * 100.0 / total)),
                Map.of("status", "SAMPLE_COLLECTED", "count", orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.SAMPLE_COLLECTED).count(), "percentage", total == 0 ? "0%" : String.format("%.2f%%", orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.SAMPLE_COLLECTED).count() * 100.0 / total)),
                Map.of("status", "IN_PROGRESS", "count", orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.IN_PROGRESS).count(), "percentage", total == 0 ? "0%" : String.format("%.2f%%", orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.IN_PROGRESS).count() * 100.0 / total)),
                Map.of("status", "COMPLETED", "count", orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.COMPLETED).count(), "percentage", total == 0 ? "0%" : String.format("%.2f%%", orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.COMPLETED).count() * 100.0 / total)),
                Map.of("status", "CANCELLED", "count", orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.CANCELLED).count(), "percentage", total == 0 ? "0%" : String.format("%.2f%%", orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.CANCELLED).count() * 100.0 / total))
        );

        var payload = Map.<String, Object>of(
                "totalOrders", total,
                "statusBreakdown", breakdown
        );

        return ApiResponse.success(HttpStatus.OK.value(), "By status statistics", payload);
    }

    @GetMapping("/revenue")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> revenue(@RequestParam(defaultValue = "month") String period) {
        var orders = orderTaskManagementService.getServiceOrders();
        double totalRevenue = orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.COMPLETED).mapToDouble(o -> o.getFinalAmount() != null ? o.getFinalAmount().doubleValue() : 0.0).sum();
        long completedOrders = orders.stream().filter(o -> o.getOrderStatus() == ServiceOrderStatus.COMPLETED).count();
        double averageOrderValue = completedOrders == 0 ? 0 : totalRevenue / completedOrders;

        Map<String, Object> periodRevenue = Map.of(
                "totalRevenue", totalRevenue,
                "completedOrders", completedOrders,
                "averageOrderValue", averageOrderValue,
                "formattedTotal", String.format("%,.0f VNĐ", totalRevenue)
        );

        Map<String, Object> monthlyRevenue = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            monthlyRevenue.put(LocalDate.now().minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM")), 0);
        }

        Map<String, Object> payload = Map.of(
                "period", period,
                "startDate", LocalDate.now().withDayOfMonth(1).toString(),
                "endDate", LocalDate.now().toString(),
                "periodRevenue", periodRevenue,
                "totalRevenue", periodRevenue,
                "monthlyRevenue", monthlyRevenue
        );

        return ApiResponse.success(HttpStatus.OK.value(), "Revenue statistics", payload);
    }

    @GetMapping("/reports")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ListReportsResponse> reports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Delegate to existing report controller logic via service
        var allReports = systemReportService.getAllSystemReports();

        var filteredReports = allReports.stream()
                .filter(r -> "all" .equals(status) || r.getReportStatus().name().equalsIgnoreCase(status))
                .toList();

        int total = filteredReports.size();
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);

        Map<String, Object> pagination = Map.of(
                "currentPage", page,
                "pageSize", size,
                "totalPages", (int) Math.ceil((double) total / size),
                "totalReports", total
        );

        ReportStatsResponse stats = new ReportStatsResponse();
        stats.setTotalReports(total);
        stats.setGeneratedReports(filteredReports.stream().filter(r -> r.getReportStatus() == ReportStatus.GENERATED).count());
        stats.setApprovedReports(filteredReports.stream().filter(r -> r.getReportStatus() == ReportStatus.APPROVED).count());
        stats.setRejectedReports(filteredReports.stream().filter(r -> r.getReportStatus() == ReportStatus.REJECTED).count());

        var response = ListReportsResponse.builder()
                .reports(filteredReports.subList(start, end))
                .reportStats(stats)
                .pagination(pagination)
                .filters(Map.of("status", status, "sortBy", sortBy, "sortDir", sortDir))
                .allRoles(RoleType.values())
                .reportStatuses(ReportStatus.values())
                .reportTypes(ReportType.values())
                .build();

        return ApiResponse.success(HttpStatus.OK.value(), "Reports retrieved", response);
    }

    @GetMapping("/reports/{reportId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<SystemReportResponse> reportDetails(@PathVariable Long reportId) {
        var report = systemReportService.getSystemReportByReportId(reportId);
        return ApiResponse.success(HttpStatus.OK.value(), "Report details retrieved", report);
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SystemReportResponse> create(@Valid @RequestBody NewReportRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var existingUser = userRepository.findByUsername(auth.getName());
        String currentUserId = existingUser
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS)).getId();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        request.setGeneratedByUser(currentUser);
        var result = systemReportService.createNewReport(request);
        return ApiResponse.success(HttpStatus.CREATED.value(), "Report created successfully", result);
    }

    @PatchMapping("/reports/{reportId}/status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updateStatus(@PathVariable Long reportId,
            @Valid @RequestBody UpdateReportStatusRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        var existingUser = userRepository.findByUsername(auth.getName());
        String currentUserId = existingUser
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS)).getId();

        var currentReport = systemReportService.getSystemReportByReportId(reportId);
        var updateRequest = UpdatingReportRequest.builder()
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
}
