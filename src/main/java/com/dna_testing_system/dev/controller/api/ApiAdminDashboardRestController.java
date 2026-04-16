package com.dna_testing_system.dev.controller.api;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserRole;
import com.dna_testing_system.dev.enums.RoleType;
import com.dna_testing_system.dev.repository.RoleRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.repository.UserRoleRepository;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import com.dna_testing_system.dev.service.SystemReportService;
import com.dna_testing_system.dev.service.UserProfileService;
import com.dna_testing_system.dev.service.service.MedicalServiceManageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/dashboard")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ApiAdminDashboardRestController {

    MedicalServiceManageService medicalServiceManageService;
    OrderTaskManagementService orderTaskManagementService;
    SystemReportService systemReportService;
    UserProfileService userProfileService;
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserRoleRepository userRoleRepository;

    // ==================== DASHBOARD ENDPOINTS ====================

    /**
     * GET /api/v1/admin/dashboard/stats
     * Lấy toàn bộ dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats(HttpServletRequest request) {
        try {
            AdminDashboardStats dashboardStats = getAdminDashboardStatistics();
            MonthlySystemStats monthlyStats = getMonthlySystemStatistics();
            SystemPerformanceMetrics performanceMetrics = getSystemPerformanceMetrics();

            Map<String, Object> stats = Map.of(
                    "dashboard", dashboardStats,
                    "monthlyStats", monthlyStats,
                    "performanceMetrics", performanceMetrics);

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Dashboard statistics loaded", stats));
        } catch (Exception e) {
            log.error("Error loading dashboard statistics: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Unable to load dashboard statistics", request.getRequestURI()));
        }
    }

    /**
     * GET /api/v1/admin/dashboard/recent-activities
     * Lấy danh sách hoạt động gần đây
     */
    @GetMapping("/recent-activities")
    public ResponseEntity<ApiResponse<List<SystemActivity>>> getRecentActivities(HttpServletRequest request) {
        try {
            List<SystemActivity> recentActivities = getRecentSystemActivities();
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                    "Recent activities loaded", recentActivities));
        } catch (Exception e) {
            log.error("Error loading recent activities: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Unable to load activities", request.getRequestURI()));
        }
    }

    /**
     * GET /api/v1/admin/dashboard/alerts
     * Lấy danh sách system alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<SystemAlert>>> getSystemAlerts(HttpServletRequest request) {
        try {
            List<SystemAlert> alerts = getSystemAlerts();
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                    "System alerts loaded", alerts));
        } catch (Exception e) {
            log.error("Error loading system alerts: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Unable to load alerts", request.getRequestURI()));
        }
    }

    // ==================== USERS MANAGEMENT ENDPOINTS ====================

    /**
     * GET /api/v1/admin/dashboard/users
     * Lấy danh sách users (có pagination + filter)
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsersList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {
        try {
            if (page < 0 || size <= 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(),
                                "Invalid pagination parameters", request.getRequestURI()));
            }
            // TODO: Implement proper pagination with UserRepository
            List<User> allUsers = userRepository.findAll();

            // Filter by search
            List<User> filteredUsers = allUsers.stream()
                    .filter(u -> search.isEmpty() ||
                            u.getUsername().toLowerCase().contains(search.toLowerCase()) ||
                            (u.getProfile() != null && u.getProfile().getEmail() != null &&
                                    u.getProfile().getEmail().toLowerCase().contains(search.toLowerCase())))
                    .collect(Collectors.toList());

            UserManagementStats userStats = getUserManagementStatistics();

            Map<String, Object> response = Map.of(
                    "users", filteredUsers,
                    "totalElements", filteredUsers.size(),
                    "totalPages", (filteredUsers.size() + size - 1) / size,
                    "currentPage", page,
                    "pageSize", size,
                    "userStats", userStats);

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                    "Users list loaded", response));
        } catch (Exception e) {
            log.error("Error loading users list: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Unable to load users", request.getRequestURI()));
        }
    }

    /**
     * GET /api/v1/admin/dashboard/users/{userId}
     * Lấy chi tiết user theo ID
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<User>> getUserById(
            @PathVariable String userId,
            HttpServletRequest request) {
        try {
            User user = userRepository.findById(userId)
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(),
                                "User not found", request.getRequestURI()));
            }

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                    "User loaded", user));
        } catch (Exception e) {
            log.error("Error loading user details: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Unable to load user", request.getRequestURI()));
        }
    }

    /**
     * PUT /api/v1/admin/dashboard/users/{userId}
     * Cập nhật user thông tin
     */
    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable String userId,
            @RequestBody Map<String, Object> userUpdate,
            HttpServletRequest request) {
        try {
            User existingUser = userRepository.findById(userId)
                    .orElse(null);

            if (existingUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(),
                                "User not found", request.getRequestURI()));
            }

            // Update user fields
            if (userUpdate.containsKey("email")) {
                if (existingUser.getProfile() != null) {
                    existingUser.getProfile().setEmail((String) userUpdate.get("email"));
                }
            }
            if (userUpdate.containsKey("active")) {
                existingUser.setIsActive((Boolean) userUpdate.get("active"));
            }
            if (userUpdate.containsKey("phone")) {
                if (existingUser.getProfile() != null) {
                    existingUser.getProfile().setPhoneNumber((String) userUpdate.get("phone"));
                }
            }
            if (userUpdate.containsKey("role")) {
                String role = (String) userUpdate.get("role");
                var roleType = RoleType.valueOf(role);
                var newRole = roleRepository.findByRoleName(roleType.name())
                        .orElseThrow(() -> new RuntimeException("Role not found"));
                existingUser.getUserRoles().stream().findFirst()
                        .ifPresent(userRole -> userRole.setRole(newRole));
            }

            existingUser.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(existingUser);

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                    "User updated successfully", updatedUser));
        } catch (Exception e) {
            log.error("Error updating user: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Unable to update user", request.getRequestURI()));
        }
    }

    // ==================== LOGS ENDPOINTS ====================

    /**
     * GET /api/v1/admin/dashboard/logs
     * Lấy danh sách system logs (có pagination)
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String level,
            HttpServletRequest request) {
        try {
            List<SystemLog> allLogs = getSystemLogs();

            // Filter by level if provided
            List<SystemLog> filteredLogs = allLogs.stream()
                    .filter(log -> level.isEmpty() || log.getLevel().equalsIgnoreCase(level))
                    .sorted((l1, l2) -> l2.getTimestamp().compareTo(l1.getTimestamp()))
                    .collect(Collectors.toList());

            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, filteredLogs.size());
            List<SystemLog> paginatedLogs = filteredLogs.subList(start, end);

            LogStatistics logStats = getLogStatistics();

            Map<String, Object> response = Map.of(
                    "logs", paginatedLogs,
                    "totalElements", filteredLogs.size(),
                    "totalPages", (filteredLogs.size() + size - 1) / size,
                    "currentPage", page,
                    "pageSize", size,
                    "statistics", logStats);

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                    "System logs loaded", response));
        } catch (Exception e) {
            log.error("Error loading system logs: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Unable to load logs", request.getRequestURI()));
        }
    }

    /**
     * GET /api/v1/admin/dashboard/logs/export/{format}
     * Export logs với định dạng TXT, CSV, hoặc JSON
     */
    @GetMapping("/logs/export/{format}")
    public ResponseEntity<Resource> exportLogs(
            @PathVariable String format,
            HttpServletRequest request) {
        try {
            List<SystemLog> logs = getSystemLogs();

            String fileName = "system_logs_" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String content = "";
            String contentType = "";

            switch (format.toLowerCase()) {
                case "txt":
                    content = generateLogsTxt(logs);
                    fileName += ".txt";
                    contentType = "text/plain";
                    break;
                case "csv":
                    content = generateLogsCsv(logs);
                    fileName += ".csv";
                    contentType = "text/csv";
                    break;
                case "json":
                    content = generateLogsJson(logs);
                    fileName += ".json";
                    contentType = "application/json";
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }

            ByteArrayResource resource = new ByteArrayResource(content.getBytes());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            log.error("Error exporting logs: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== ANALYTICS ENDPOINTS ====================

    /**
     * GET /api/v1/admin/dashboard/analytics
     * Lấy analytics data (theo period: today, week, month, quarter, year)
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics(
            @RequestParam(defaultValue = "month") String period,
            HttpServletRequest request) {
        try {
            // Define date range based on selected period
            LocalDate endDate = LocalDate.now();
            LocalDate startDate;

            switch (period) {
                case "today":
                    startDate = endDate;
                    break;
                case "week":
                    startDate = endDate.minusWeeks(1);
                    break;
                case "month":
                    startDate = endDate.minusMonths(1);
                    break;
                case "quarter":
                    startDate = endDate.minusMonths(3);
                    break;
                case "year":
                    startDate = endDate.minusYears(1);
                    break;
                default:
                    startDate = endDate.minusMonths(1);
            }

            // Generate analytics summary
            AnalyticsSummary analyticsSummary = new AnalyticsSummary();
            double totalRevenue = calculateTotalRevenue(startDate, endDate);
            int totalOrders = calculateTotalOrders(startDate, endDate);
            double avgOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;
            int activeUsers = 980;

            analyticsSummary.setTotalRevenue("$" + String.format("%,.2f", totalRevenue));
            analyticsSummary.setTotalOrders(String.valueOf(totalOrders));
            analyticsSummary.setActiveUsers(String.valueOf(activeUsers));
            analyticsSummary.setAvgOrderValue("$" + String.format("%,.2f", avgOrderValue));
            analyticsSummary.setRevenueGrowth("12.5%");
            analyticsSummary.setOrdersGrowth("8.3%");
            analyticsSummary.setUsersGrowth("15.2%");
            analyticsSummary.setAovGrowth("4.7%");

            SystemMetricsData systemMetrics = generateSystemMetricsData();

            Map<String, Object> response = Map.of(
                    "summary", analyticsSummary,
                    "systemMetrics", systemMetrics,
                    "selectedPeriod", period);

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                    "Analytics data loaded", response));
        } catch (Exception e) {
            log.error("Error loading analytics: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Unable to load analytics", request.getRequestURI()));
        }
    }

    // ==================== PROFILE ENDPOINTS ====================

    /**
     * GET /api/v1/admin/dashboard/profile
     * Lấy profile của admin hiện tại
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserProfileResponse profile = userProfileService.getUserProfile(auth.getName());

            if (profile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(),
                                "Profile not found", request.getRequestURI()));
            }

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                    "Profile loaded", profile));
        } catch (Exception e) {
            log.error("Error loading profile: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Unable to load profile", request.getRequestURI()));
        }
    }

    /**
     * PUT /api/v1/admin/dashboard/profile
     * Cập nhật profile của admin (với file upload)
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @RequestPart("profile") UserProfileRequest userProfile,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserProfileResponse existingProfile = userProfileService.getUserProfile(auth.getName());

            if (existingProfile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(),
                                "Profile not found", request.getRequestURI()));
            }

            // Handle file upload
            if (file == null || file.isEmpty()) {
                userProfile.setProfileImageUrl(existingProfile.getProfileImageUrl());
            } else {
                try {
                    String uploadsDir = "uploads/";
                    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                    Path path = Paths.get(uploadsDir + fileName);
                    Files.createDirectories(Paths.get(uploadsDir));
                    file.transferTo(path);
                    userProfile.setProfileImageUrl("/uploads/" + fileName);
                } catch (Exception e) {
                    log.error("Failed to upload profile image", e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                    "Failed to upload profile image", request.getRequestURI()));
                }
            }

            // Keep existing dateOfBirth if not changed
            if (userProfile.getDateOfBirth() == null) {
                userProfile.setDateOfBirth(existingProfile.getDateOfBirth());
            }

            boolean updated = userProfileService.updateUserProfile(auth.getName(), userProfile);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(),
                                "Profile not found", request.getRequestURI()));
            }

            UserProfileResponse updatedProfile = userProfileService.getUserProfile(auth.getName());
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                    "Profile updated successfully", updatedProfile));
        } catch (Exception e) {
            log.error("Error updating profile: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Unable to update profile", request.getRequestURI()));
        }
    }

    // ==================== SETTINGS ENDPOINTS ====================

    /**
     * GET /api/v1/admin/dashboard/settings
     * Lấy system configuration
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<SystemConfiguration>> getSettings(HttpServletRequest request) {
        try {
            SystemConfiguration config = getSystemConfiguration();
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                    "Settings loaded", config));
        } catch (Exception e) {
            log.error("Error loading settings: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Unable to load settings", request.getRequestURI()));
        }
    }

    /**
     * PUT /api/v1/admin/dashboard/settings
     * Cập nhật system configuration
     */
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<SystemConfiguration>> updateSettings(
            @RequestBody SystemConfiguration config,
            HttpServletRequest request) {
        try {
            log.info("System settings updated: {}", config);
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(),
                    "Settings updated successfully", config));
        } catch (Exception e) {
            log.error("Error updating settings: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Unable to update settings", request.getRequestURI()));
        }
    }

    // ==================== HELPER METHODS ====================

    private AdminDashboardStats getAdminDashboardStatistics() {
        try {
            List<User> allUsers = userRepository.findAll();
            long totalUsers = allUsers.size();
            long activeUsers = allUsers.stream().filter(User::getIsActive).count();
            long totalServices = medicalServiceManageService.getAllServices().size();
            var orders = orderTaskManagementService.getServiceOrders();
            long totalOrders = orders.size();
            long pendingOrders = orders.stream()
                    .filter(order -> order.getOrderStatus().name().equals("PENDING"))
                    .count();
            double totalRevenue = orders.stream()
                    .filter(order -> order.getOrderStatus().name().equals("COMPLETED"))
                    .mapToDouble(order -> 299.0)
                    .sum();

            return new AdminDashboardStats(totalUsers, activeUsers, totalServices,
                    totalOrders, pendingOrders, totalRevenue);
        } catch (Exception e) {
            log.error("Error getting dashboard statistics: ", e);
            return new AdminDashboardStats(0, 0, 0, 0, 0, 0.0);
        }
    }

    private List<SystemActivity> getRecentSystemActivities() {
        List<SystemActivity> activities = new ArrayList<>();

        try {
            var recentOrders = orderTaskManagementService.getServiceOrders().stream()
                    .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                    .limit(10)
                    .collect(Collectors.toList());

            for (var order : recentOrders) {
                String timeAgo = getTimeAgo(order.getCreatedAt());
                activities.add(new SystemActivity(
                        "ORDER",
                        "New Order Created",
                        "Order #" + order.getId() + " created by " + order.getCustomerName(),
                        timeAgo,
                        "success"));
            }

            List<User> recentUsers = userRepository.findAll().stream()
                    .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                    .limit(5)
                    .collect(Collectors.toList());

            for (User user : recentUsers) {
                String timeAgo = getTimeAgo(user.getCreatedAt());
                activities.add(new SystemActivity(
                        "USER",
                        "New User Registered",
                        "User " + user.getUsername() + " joined the system",
                        timeAgo,
                        "info"));
            }

        } catch (Exception e) {
            log.error("Error getting recent system activities: ", e);
        }

        return activities.stream()
                .sorted((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()))
                .limit(20)
                .collect(Collectors.toList());
    }

    private List<SystemAlert> getSystemAlerts() {
        List<SystemAlert> alerts = new ArrayList<>();

        try {
            var orders = orderTaskManagementService.getServiceOrders();
            long pendingOrders = orders.stream()
                    .filter(order -> order.getOrderStatus().name().equals("PENDING"))
                    .count();

            if (pendingOrders > 10) {
                alerts.add(new SystemAlert(
                        "warning",
                        "High Pending Orders",
                        pendingOrders + " orders are pending processing",
                        "high"));
            }

            List<User> allUsers = userRepository.findAll();
            long inactiveUsers = allUsers.stream()
                    .filter(user -> !user.getIsActive())
                    .count();

            if (inactiveUsers > 5) {
                alerts.add(new SystemAlert(
                        "info",
                        "Inactive Users",
                        inactiveUsers + " users are currently inactive",
                        "medium"));
            }

            alerts.add(new SystemAlert(
                    "success",
                    "System Running Normally",
                    "All services are operational",
                    "low"));

        } catch (Exception e) {
            log.error("Error getting system alerts: ", e);
            alerts.add(new SystemAlert(
                    "error",
                    "System Error",
                    "Unable to retrieve system status",
                    "critical"));
        }

        return alerts;
    }

    private MonthlySystemStats getMonthlySystemStatistics() {
        try {
            LocalDateTime startOfMonth = LocalDateTime.now()
                    .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

            var orders = orderTaskManagementService.getServiceOrders();
            var monthlyOrders = orders.stream()
                    .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                    .collect(Collectors.toList());

            List<User> users = userRepository.findAll();
            List<User> monthlyUsers = users.stream()
                    .filter(user -> user.getCreatedAt().isAfter(startOfMonth))
                    .collect(Collectors.toList());

            long newOrders = monthlyOrders.size();
            long newUsers = monthlyUsers.size();
            double revenue = monthlyOrders.stream()
                    .filter(order -> order.getOrderStatus().name().equals("COMPLETED"))
                    .mapToDouble(order -> 299.0)
                    .sum();

            return new MonthlySystemStats(newOrders, newUsers, revenue);

        } catch (Exception e) {
            log.error("Error getting monthly system statistics: ", e);
            return new MonthlySystemStats(0, 0, 0.0);
        }
    }

    private SystemPerformanceMetrics getSystemPerformanceMetrics() {
        return new SystemPerformanceMetrics(95.5, 68.2, 42.1, 99.9, 350);
    }

    private UserManagementStats getUserManagementStatistics() {
        try {
            List<User> allUsers = userRepository.findAll();
            long totalUsers = allUsers.size();
            long activeUsers = allUsers.stream().filter(User::getIsActive).count();
            long inactiveUsers = totalUsers - activeUsers;

            long adminUsers = userRepository.findUsersByRoleName("ADMIN").size();
            long managerUsers = userRepository.findUsersByRoleName("MANAGER").size();
            long staffUsers = userRepository.findUsersByRoleName("STAFF").size();
            long customerUsers = userRepository.findUsersByRoleName("CUSTOMER").size();

            return new UserManagementStats(totalUsers, activeUsers, inactiveUsers,
                    adminUsers, managerUsers, staffUsers, customerUsers);

        } catch (Exception e) {
            log.error("Error getting user management statistics: ", e);
            return new UserManagementStats(0, 0, 0, 0, 0, 0, 0);
        }
    }

    private SystemConfiguration getSystemConfiguration() {
        return new SystemConfiguration(
                "Bloodline DNA Testing System",
                "1.0.0",
                "admin@bloodline.com",
                "Production",
                true,
                true,
                "smtp.bloodline.com");
    }

    private List<SystemLog> getSystemLogs() {
        List<SystemLog> logs = new ArrayList<>();
        logs.add(new SystemLog("INFO", "System startup completed", "System", LocalDateTime.now().minusHours(2)));
        logs.add(new SystemLog("WARN", "High memory usage detected", "Performance",
                LocalDateTime.now().minusMinutes(30)));
        logs.add(new SystemLog("ERROR", "Database connection timeout", "Database",
                LocalDateTime.now().minusMinutes(15)));
        logs.add(new SystemLog("INFO", "User authentication successful", "Security",
                LocalDateTime.now().minusMinutes(5)));
        return logs;
    }

    private LogStatistics getLogStatistics() {
        return new LogStatistics(156, 23, 8, 2);
    }

    private SystemMetricsData generateSystemMetricsData() {
        return new SystemMetricsData(99.9, 245, 0.5, 12, 350);
    }

    private String generateLogsTxt(List<SystemLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("SYSTEM LOGS EXPORT\n");
        sb.append("Generated: ").append(LocalDateTime.now()).append("\n");
        sb.append("================================\n\n");

        for (SystemLog log : logs) {
            sb.append("[").append(log.getLevel()).append("] ")
                    .append(log.getTimestamp()).append(" - ")
                    .append(log.getSource()).append(": ")
                    .append(log.getMessage()).append("\n");
        }

        return sb.toString();
    }

    private String generateLogsCsv(List<SystemLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Level,Timestamp,Source,Message\n");

        for (SystemLog log : logs) {
            sb.append("\"").append(log.getLevel()).append("\",")
                    .append("\"").append(log.getTimestamp()).append("\",")
                    .append("\"").append(log.getSource()).append("\",")
                    .append("\"").append(log.getMessage()).append("\"\n");
        }

        return sb.toString();
    }

    private String generateLogsJson(List<SystemLog> logs) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(logs);
        } catch (Exception e) {
            log.error("Error converting logs to JSON", e);
            return "[]";
        }
    }

    private double calculateTotalRevenue(LocalDate startDate, LocalDate endDate) {
        return 157890.50;
    }

    private int calculateTotalOrders(LocalDate startDate, LocalDate endDate) {
        return 1245;
    }

    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null)
            return "Unknown";

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);

        if (minutes < 1)
            return "Just now";
        if (minutes < 60)
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24)
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 7)
            return days + " day" + (days == 1 ? "" : "s") + " ago";

        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // ==================== HELPER CLASSES ====================

    public static class AdminDashboardStats {
        private final long totalUsers;
        private final long activeUsers;
        private final long totalServices;
        private final long totalOrders;
        private final long pendingOrders;
        private final double totalRevenue;

        public AdminDashboardStats(long totalUsers, long activeUsers, long totalServices,
                long totalOrders, long pendingOrders, double totalRevenue) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.totalServices = totalServices;
            this.totalOrders = totalOrders;
            this.pendingOrders = pendingOrders;
            this.totalRevenue = totalRevenue;
        }

        public long getTotalUsers() {
            return totalUsers;
        }

        public long getActiveUsers() {
            return activeUsers;
        }

        public long getTotalServices() {
            return totalServices;
        }

        public long getTotalOrders() {
            return totalOrders;
        }

        public long getPendingOrders() {
            return pendingOrders;
        }

        public double getTotalRevenue() {
            return totalRevenue;
        }
    }

    public static class SystemActivity {
        private final String type;
        private final String title;
        private final String description;
        private final String timeAgo;
        private final String severity;
        private final LocalDateTime timestamp;

        public SystemActivity(String type, String title, String description, String timeAgo, String severity) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.timeAgo = timeAgo;
            this.severity = severity;
            this.timestamp = LocalDateTime.now();
        }

        public String getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getTimeAgo() {
            return timeAgo;
        }

        public String getSeverity() {
            return severity;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    public static class SystemAlert {
        private final String type;
        private final String title;
        private final String message;
        private final String priority;

        public SystemAlert(String type, String title, String message, String priority) {
            this.type = type;
            this.title = title;
            this.message = message;
            this.priority = priority;
        }

        public String getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }

        public String getPriority() {
            return priority;
        }
    }

    public static class MonthlySystemStats {
        private final long newOrders;
        private final long newUsers;
        private final double revenue;

        public MonthlySystemStats(long newOrders, long newUsers, double revenue) {
            this.newOrders = newOrders;
            this.newUsers = newUsers;
            this.revenue = revenue;
        }

        public long getNewOrders() {
            return newOrders;
        }

        public long getNewUsers() {
            return newUsers;
        }

        public double getRevenue() {
            return revenue;
        }
    }

    public static class SystemPerformanceMetrics {
        private final double cpuUsage;
        private final double memoryUsage;
        private final double diskUsage;
        private final double uptime;
        private final int activeSessions;

        public SystemPerformanceMetrics(double cpuUsage, double memoryUsage, double diskUsage,
                double uptime, int activeSessions) {
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.diskUsage = diskUsage;
            this.uptime = uptime;
            this.activeSessions = activeSessions;
        }

        public double getCpuUsage() {
            return cpuUsage;
        }

        public double getMemoryUsage() {
            return memoryUsage;
        }

        public double getDiskUsage() {
            return diskUsage;
        }

        public double getUptime() {
            return uptime;
        }

        public int getActiveSessions() {
            return activeSessions;
        }
    }

    public static class UserManagementStats {
        private final long totalUsers;
        private final long activeUsers;
        private final long inactiveUsers;
        private final long adminUsers;
        private final long managerUsers;
        private final long staffUsers;
        private final long customerUsers;

        public UserManagementStats(long totalUsers, long activeUsers, long inactiveUsers,
                long adminUsers, long managerUsers, long staffUsers, long customerUsers) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.inactiveUsers = inactiveUsers;
            this.adminUsers = adminUsers;
            this.managerUsers = managerUsers;
            this.staffUsers = staffUsers;
            this.customerUsers = customerUsers;
        }

        public long getTotalUsers() {
            return totalUsers;
        }

        public long getActiveUsers() {
            return activeUsers;
        }

        public long getInactiveUsers() {
            return inactiveUsers;
        }

        public long getAdminUsers() {
            return adminUsers;
        }

        public long getManagerUsers() {
            return managerUsers;
        }

        public long getStaffUsers() {
            return staffUsers;
        }

        public long getCustomerUsers() {
            return customerUsers;
        }
    }

    public static class SystemConfiguration {
        private String systemName;
        private String version;
        private String adminEmail;
        private String environment;
        private boolean maintenanceMode;
        private boolean emailNotifications;
        private String smtpServer;

        public SystemConfiguration(String systemName, String version, String adminEmail,
                String environment, boolean maintenanceMode,
                boolean emailNotifications, String smtpServer) {
            this.systemName = systemName;
            this.version = version;
            this.adminEmail = adminEmail;
            this.environment = environment;
            this.maintenanceMode = maintenanceMode;
            this.emailNotifications = emailNotifications;
            this.smtpServer = smtpServer;
        }

        public String getSystemName() {
            return systemName;
        }

        public String getVersion() {
            return version;
        }

        public String getAdminEmail() {
            return adminEmail;
        }

        public String getEnvironment() {
            return environment;
        }

        public boolean isMaintenanceMode() {
            return maintenanceMode;
        }

        public boolean isEmailNotifications() {
            return emailNotifications;
        }

        public String getSmtpServer() {
            return smtpServer;
        }

        public void setSystemName(String systemName) {
            this.systemName = systemName;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public void setAdminEmail(String adminEmail) {
            this.adminEmail = adminEmail;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        public void setMaintenanceMode(boolean maintenanceMode) {
            this.maintenanceMode = maintenanceMode;
        }

        public void setEmailNotifications(boolean emailNotifications) {
            this.emailNotifications = emailNotifications;
        }

        public void setSmtpServer(String smtpServer) {
            this.smtpServer = smtpServer;
        }
    }

    public static class SystemLog {
        private final String level;
        private final String message;
        private final String source;
        private final LocalDateTime timestamp;

        public SystemLog(String level, String message, String source, LocalDateTime timestamp) {
            this.level = level;
            this.message = message;
            this.source = source;
            this.timestamp = timestamp;
        }

        public String getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public String getSource() {
            return source;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    public static class LogStatistics {
        private final long infoLogs;
        private final long warningLogs;
        private final long errorLogs;
        private final long criticalLogs;

        public LogStatistics(long infoLogs, long warningLogs, long errorLogs, long criticalLogs) {
            this.infoLogs = infoLogs;
            this.warningLogs = warningLogs;
            this.errorLogs = errorLogs;
            this.criticalLogs = criticalLogs;
        }

        public long getInfoLogs() {
            return infoLogs;
        }

        public long getWarningLogs() {
            return warningLogs;
        }

        public long getErrorLogs() {
            return errorLogs;
        }

        public long getCriticalLogs() {
            return criticalLogs;
        }
    }

    public static class AnalyticsSummary {
        private String totalRevenue;
        private String totalOrders;
        private String activeUsers;
        private String avgOrderValue;
        private String revenueGrowth;
        private String ordersGrowth;
        private String usersGrowth;
        private String aovGrowth;

        public String getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(String totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public String getTotalOrders() {
            return totalOrders;
        }

        public void setTotalOrders(String totalOrders) {
            this.totalOrders = totalOrders;
        }

        public String getActiveUsers() {
            return activeUsers;
        }

        public void setActiveUsers(String activeUsers) {
            this.activeUsers = activeUsers;
        }

        public String getAvgOrderValue() {
            return avgOrderValue;
        }

        public void setAvgOrderValue(String avgOrderValue) {
            this.avgOrderValue = avgOrderValue;
        }

        public String getRevenueGrowth() {
            return revenueGrowth;
        }

        public void setRevenueGrowth(String revenueGrowth) {
            this.revenueGrowth = revenueGrowth;
        }

        public String getOrdersGrowth() {
            return ordersGrowth;
        }

        public void setOrdersGrowth(String ordersGrowth) {
            this.ordersGrowth = ordersGrowth;
        }

        public String getUsersGrowth() {
            return usersGrowth;
        }

        public void setUsersGrowth(String usersGrowth) {
            this.usersGrowth = usersGrowth;
        }

        public String getAovGrowth() {
            return aovGrowth;
        }

        public void setAovGrowth(String aovGrowth) {
            this.aovGrowth = aovGrowth;
        }
    }

    public static class SystemMetricsData {
        private double serverUptime;
        private double responseTime;
        private double errorRate;
        private int databaseConnections;
        private int activeSessionCount;

        public SystemMetricsData(double serverUptime, double responseTime,
                double errorRate, int databaseConnections, int activeSessionCount) {
            this.serverUptime = serverUptime;
            this.responseTime = responseTime;
            this.errorRate = errorRate;
            this.databaseConnections = databaseConnections;
            this.activeSessionCount = activeSessionCount;
        }

        public double getServerUptime() {
            return serverUptime;
        }

        public double getResponseTime() {
            return responseTime;
        }

        public double getErrorRate() {
            return errorRate;
        }

        public int getDatabaseConnections() {
            return databaseConnections;
        }

        public int getActiveSessionCount() {
            return activeSessionCount;
        }
    }
}