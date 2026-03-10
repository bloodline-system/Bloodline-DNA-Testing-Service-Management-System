package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.MedicalServiceResponse;
import com.dna_testing_system.dev.dto.response.ServiceOrderResponse;
import com.dna_testing_system.dev.dto.response.SystemReportResponse;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.entity.Role;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserRole;
import com.dna_testing_system.dev.enums.RoleType;
import com.dna_testing_system.dev.repository.RoleRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.repository.UserRoleRepository;
import com.dna_testing_system.dev.service.MedicalServiceManageService;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import com.dna_testing_system.dev.service.SystemReportService;
import com.dna_testing_system.dev.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminDashboardController extends BaseController {

    MedicalServiceManageService medicalServiceManageService;
    OrderTaskManagementService orderTaskManagementService;
    SystemReportService systemReportService;
    UserProfileService userProfileService;
    UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        addUserProfileToModel(model);
        model.addAttribute("pageTitle", "Admin Dashboard");

        try {
            // Get admin dashboard statistics
            AdminDashboardStats dashboardStats = getAdminDashboardStatistics();
            model.addAttribute("dashboardStats", dashboardStats);

            // Get recent system activities
            List<SystemActivity> recentActivities = getRecentSystemActivities();
            model.addAttribute("recentActivities", recentActivities);

            // Get system alerts
            List<SystemAlert> systemAlerts = getSystemAlerts();
            model.addAttribute("systemAlerts", systemAlerts);

            // Get monthly statistics
            MonthlySystemStats monthlyStats = getMonthlySystemStatistics();
            model.addAttribute("monthlyStats", monthlyStats);

            // Get performance metrics
            SystemPerformanceMetrics performanceMetrics = getSystemPerformanceMetrics();
            model.addAttribute("performanceMetrics", performanceMetrics);

        } catch (Exception e) {
            log.error("Error loading admin dashboard data: ", e);
            model.addAttribute("error", "Unable to load dashboard data");
        }
        
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "all") String role,
                        @RequestParam(defaultValue = "all") String status,
                        @RequestParam(defaultValue = "") String search,
                        @RequestParam(defaultValue = "createdAt") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDir,
                        Model model) {
        addUserProfileToModel(model);
        model.addAttribute("pageTitle", "User Management");

        // Add breadcrumbs
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(new BreadcrumbItem("User Management", ""));
        model.addAttribute("breadcrumbs", breadcrumbs);

        try {
            // Get all user profiles with pagination and filtering
            List<UserProfileResponse> allUsers = userProfileService.getUserProfiles();

            // Apply filters
            List<UserProfileResponse> filteredUsers = allUsers.stream()
                    .filter(user -> {
                        // Role filter
                        if (!"all".equals(role) && !role.isEmpty()) {
                            return user.getRole() != null && user.getRole().equalsIgnoreCase(role);
                        }
                        return true;
                    })
                    .filter(user -> {
                        // Status filter
                        if (!"all".equals(status) && !status.isEmpty()) {
                            boolean isActive = "active".equals(status);
                            return user.isActive() == isActive;
                        }
                        return true;
                    })
                    .filter(user -> {
                        // Search filter
                        if (!search.isEmpty()) {
                            String searchLower = search.toLowerCase();
                            return (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(searchLower)) ||
                                    (user.getLastName() != null && user.getLastName().toLowerCase().contains(searchLower)) ||
                                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchLower)) ||
                                    (user.getUsername() != null && user.getUsername().toLowerCase().contains(searchLower));
                        }
                        return true;
                    })
                    .sorted((u1, u2) -> {
                        int result = 0;
                        switch (sortBy) {
                            case "firstName":
                                result = (u1.getFirstName() != null ? u1.getFirstName() : "").compareTo(
                                        u2.getFirstName() != null ? u2.getFirstName() : "");
                                break;
                            case "lastName":
                                result = (u1.getLastName() != null ? u1.getLastName() : "").compareTo(
                                        u2.getLastName() != null ? u2.getLastName() : "");
                                break;
                            case "username":
                                result = (u1.getUsername() != null ? u1.getUsername() : "").compareTo(
                                        u2.getUsername() != null ? u2.getUsername() : "");
                                break;
                            case "email":
                                result = (u1.getEmail() != null ? u1.getEmail() : "").compareTo(
                                        u2.getEmail() != null ? u2.getEmail() : "");
                                break;
                            case "role":
                                result = (u1.getRole() != null ? u1.getRole() : "").compareTo(
                                        u2.getRole() != null ? u2.getRole() : "");
                                break;
                            case "createdAt":
                            default:
                                result = u1.getCreatedAt() != null && u2.getCreatedAt() != null ?
                                        u1.getCreatedAt().compareTo(u2.getCreatedAt()) : 0;
                                break;
                        }
                        return "desc".equals(sortDir) ? -result : result;
                    })
                    .collect(Collectors.toList());

            // Implement simple pagination
            int start = page * size;
            int end = Math.min(start + size, filteredUsers.size());
            List<UserProfileResponse> paginatedUsers = start < filteredUsers.size() ?
                    filteredUsers.subList(start, end) : new ArrayList<>();

            model.addAttribute("users", paginatedUsers);
            model.addAttribute("currentPage", page);
            model.addAttribute("size", size);
            model.addAttribute("totalPages", (int) Math.ceil((double) filteredUsers.size() / size));
            model.addAttribute("totalElements", filteredUsers.size());
            model.addAttribute("selectedRole", role);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("searchTerm", search);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);

            // Get user statistics
            UserManagementStats userStats = getUserManagementStatistics();
            model.addAttribute("userStats", userStats);

            var monthlyStats = getMonthlySystemStatistics();
            model.addAttribute("monthlyStats", monthlyStats);

            // Add empty user object for the add form
            model.addAttribute("newUser", new User());
            model.addAttribute("newUserProfile", new UserProfileRequest());

        } catch (Exception e) {
            log.error("Error loading user management data: ", e);
            model.addAttribute("error", "Unable to load user data");
            model.addAttribute("users", new ArrayList<>());
            model.addAttribute("totalElements", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("currentPage", 0);
        }

        return "admin/users";
    }
//
//    @PostMapping("/users")
//    public String createUser(@ModelAttribute User newUser,
//                             @RequestParam String firstName,
//                             @RequestParam String lastName,
//                             @RequestParam(required = false) String phone,
//                             @RequestParam String role,
//                             RedirectAttributes redirectAttributes) {
//        try {
//            // Set default values
//            newUser.setIsActive(newUser.getIsActive() != null ? newUser.getIsActive() : true);
//            newUser.setCreatedAt(LocalDateTime.now());
//            newUser.setUpdatedAt(LocalDateTime.now());
//
//            // Save user
//            User savedUser = userRepository.save(newUser);
//
//            // Create user profile
//            UserProfileRequest profileRequest = new UserProfileRequest();
//            profileRequest.setFirstName(firstName);
//            profileRequest.setLastName(lastName);
//            profileRequest.setPhoneNumber(phone);
//
//            try {
//                userProfileService.createUserProfile(savedUser.getUsername(), profileRequest);
//            } catch (Exception e) {
//                log.warn("Could not create profile for user: {}", savedUser.getUsername());
//            }
//
//            redirectAttributes.addFlashAttribute("successMessage",
//                    "User " + savedUser.getUsername() + " created successfully.");
//
//        } catch (Exception e) {
//            log.error("Error creating user: ", e);
//            redirectAttributes.addFlashAttribute("errorMessage",
//                    "Error creating user: " + e.getMessage());
//        }
//
//        return "redirect:/admin/users";
//    }

    @PostMapping("/users/{userId}/update")
    public String updateUser(@PathVariable String userId,
                             @RequestParam String firstName,
                             @RequestParam String lastName,
                             @RequestParam String email,
                             @RequestParam String role,
                             @RequestParam(required = false) String phone,
                             @RequestParam Boolean active,
                             RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            User existingUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            User admin = userRepository.findByUsername(authentication.getName()).orElseThrow(
                    () -> new RuntimeException("User not found")
            );
            // Update basic user fields
            existingUser.getUserProfile().setEmail(email);
            existingUser.setIsActive(active);
            existingUser.getUserProfile().setPhoneNumber(phone);
            existingUser.setUpdatedAt(LocalDateTime.now());
            existingUser.getUserRoles().stream().findFirst().ifPresent(userRole -> {
                var roleType =  RoleType.valueOf(role);
                var newRole = roleRepository.findByRoleName(roleType.name());
                userRole.setRole(newRole);
            });

//            if (role != null && !role.isEmpty()) {
//                // Clear existing roles and add new role
//                existingUser.getUserRoles().clear();
//
//                // Create new role (assuming you have a Role entity and repository)
//                var roleType =  RoleType.valueOf(role);
//                var newRole = roleRepository.findByRoleName(roleType.name());
//
//                UserRole userRole = UserRole.builder()
//                        .user(existingUser)
//                        .role(newRole)
//                        .isActive(true)
//                        .assignedAt(LocalDateTime.now())
//                        .assignedBy(admin.getUsername())
//                        .build();
//                userRoleRepository.save(userRole);
//
//                existingUser.getUserRoles().add(userRole);
//            }
//
//            userRepository.save(existingUser);

            // Update profile
            try {
                UserProfileRequest profileRequest = new UserProfileRequest();
                profileRequest.setFirstName(firstName);
                profileRequest.setLastName(lastName);
                profileRequest.setPhoneNumber(phone);
                profileRequest.setEmail(email);

                userProfileService.updateUserProfile(existingUser.getUsername(), profileRequest);
            } catch (Exception e) {
                log.warn("Could not update profile for user: {}", existingUser.getUsername());
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "User " + existingUser.getUsername() + " updated successfully.");

        } catch (Exception e) {
            log.error("Error updating user: ", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error updating user: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/toggle-status")
    public String toggleUserStatus(@PathVariable String userId, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setIsActive(!user.getIsActive());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            String status = user.getIsActive() ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute("successMessage",
                    "User " + user.getUsername() + " has been " + status + " successfully.");

        } catch (Exception e) {
            log.error("Error toggling user status: ", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error updating user status: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/delete")
    public String deleteUser(@PathVariable String userId, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "User not found");
                return "redirect:/admin/users";
            }

            String username = user.getUsername();
            userRepository.deleteById(userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "User " + username + " has been deleted successfully.");

        } catch (Exception e) {
            log.error("Error deleting user: ", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error deleting user: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/bulk-activate")
    public String bulkActivateUsers(@RequestParam List<String> userIds,
                                    RedirectAttributes redirectAttributes) {
        try {
            List<User> users = userRepository.findAllById(userIds);
            users.forEach(user -> {
                user.setIsActive(true);
                user.setUpdatedAt(LocalDateTime.now());
            });
            userRepository.saveAll(users);

            redirectAttributes.addFlashAttribute("successMessage",
                    users.size() + " users activated successfully.");

        } catch (Exception e) {
            log.error("Error bulk activating users: ", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error activating users: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/bulk-deactivate")
    public String bulkDeactivateUsers(@RequestParam List<String> userIds,
                                      RedirectAttributes redirectAttributes) {
        try {
            List<User> users = userRepository.findAllById(userIds);
            users.forEach(user -> {
                user.setIsActive(false);
                user.setUpdatedAt(LocalDateTime.now());
            });
            userRepository.saveAll(users);

            redirectAttributes.addFlashAttribute("successMessage",
                    users.size() + " users deactivated successfully.");

        } catch (Exception e) {
            log.error("Error bulk deactivating users: ", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error deactivating users: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/bulk-delete")
    public String bulkDeleteUsers(@RequestParam List<String> userIds,
                                  RedirectAttributes redirectAttributes) {
        try {
            userRepository.deleteAllById(userIds);

            redirectAttributes.addFlashAttribute("successMessage",
                    userIds.size() + " users deleted successfully.");

        } catch (Exception e) {
            log.error("Error bulk deleting users: ", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error deleting users: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        addUserProfileToModel(model);
        model.addAttribute("pageTitle", "System Settings");
        
        try {
            // Get system configuration
            SystemConfiguration systemConfig = getSystemConfiguration();
            model.addAttribute("systemConfig", systemConfig);

        } catch (Exception e) {
            log.error("Error loading system settings data: ", e);
            model.addAttribute("error", "Unable to load settings data");
        }
        
        return "admin/settings";
    }
    @GetMapping("/services")
    public String services(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {

            String currentPrincipalName = authentication.getName();
            UserProfileResponse userProfile = userProfileService.getUserProfile(currentPrincipalName);
            model.addAttribute("userProfile", userProfile);
        }
        model.addAttribute("pageTitle", "Services Management");
        model.addAttribute("services", medicalServiceManageService.getAllServices());
        model.addAttribute("serviceTypes", medicalServiceManageService.getAllServiceTypes());
        model.addAttribute("serviceFeatures", medicalServiceManageService.getAllServiceFeatures());
        model.addAttribute("service", medicalServiceManageService);
        return "admin/services";
    }

    @GetMapping("/logs")
    public String logs(Model model) {
        addUserProfileToModel(model);
        model.addAttribute("pageTitle", "System Logs");
        
        try {
            // Get system logs
            List<SystemLog> systemLogs = getSystemLogs();
            model.addAttribute("systemLogs", systemLogs);

            // Get log statistics
            LogStatistics logStats = getLogStatistics();
            model.addAttribute("logStats", logStats);

        } catch (Exception e) {
            log.error("Error loading system logs data: ", e);
            model.addAttribute("error", "Unable to load logs data");
        }
        
        return "admin/logs";
    }

    @GetMapping("/analytics")
    public String analytics(Model model, @RequestParam(required = false, defaultValue = "month") String period) {
        addUserProfileToModel(model);
        model.addAttribute("pageTitle", "Advanced Analytics");
        
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
                    startDate = endDate.minusMonths(1); // Default to month
            }
            
            // Generate analytics data
            AnalyticsSummary analyticsSummary = new AnalyticsSummary();
            
            // Set KPI metrics
            double totalRevenue = calculateTotalRevenue(startDate, endDate);
            int totalOrders = calculateTotalOrders(startDate, endDate);
            double avgOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;
            int activeUsers = 980; // This would be from a repository in a real implementation
            
            // Set revenue with currency format
            analyticsSummary.setTotalRevenue("$" + String.format("%,.2f", totalRevenue));
            analyticsSummary.setTotalOrders(String.valueOf(totalOrders));
            analyticsSummary.setActiveUsers(String.valueOf(activeUsers));
            analyticsSummary.setAvgOrderValue("$" + String.format("%,.2f", avgOrderValue));
            
            // Set growth percentages 
            analyticsSummary.setRevenueGrowth("12.5%");
            analyticsSummary.setOrdersGrowth("8.3%");
            analyticsSummary.setUsersGrowth("15.2%");
            analyticsSummary.setAovGrowth("4.7%");
            
            // Generate chart data
            RevenueData revenueData = generateRevenueData(startDate, endDate, period);
            OrderStatusData orderStatusData = generateOrderStatusData(startDate, endDate);
            UserActivityData userActivityData = generateUserActivityData(startDate, endDate, period);
            ServicePerformanceData servicePerformanceData = generateServicePerformanceData(startDate, endDate);
            SystemMetricsData systemMetricsData = generateSystemMetricsData();
            List<TopServiceData> topServicesData = generateTopServicesData(startDate, endDate);
            List<GeographicData> geographicData = generateGeographicData(startDate, endDate);
            
            // Convert chart data to JSON for the template
            model.addAttribute("revenueChartData", convertToChartData(revenueData));
            model.addAttribute("orderStatusChartData", convertToChartData(orderStatusData));
            model.addAttribute("userActivityChartData", convertToChartData(userActivityData));
            model.addAttribute("servicePerformanceChartData", convertToChartData(servicePerformanceData));
            
            // Add all data to model
            model.addAttribute("analytics", analyticsSummary);
            model.addAttribute("systemMetrics", systemMetricsData);
            model.addAttribute("topServices", topServicesData);
            model.addAttribute("geographicData", geographicData);
            model.addAttribute("selectedPeriod", period);
            
        } catch (Exception e) {
            log.error("Error loading analytics data: ", e);
            model.addAttribute("error", "Unable to load analytics data");
        }
        
        return "admin/analytics";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        addUserProfileToModel(model);
        model.addAttribute("pageTitle", "Admin Profile");
        return "admin/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("userEditProfile") UserProfileRequest userProfile,
                                @RequestParam(value = "file", required = false) MultipartFile file,
                                Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        UserProfileResponse existingProfile = userProfileService.getUserProfile(currentPrincipalName);

        // Handle file upload
        if (file != null && !file.getOriginalFilename().equals("")) {
            String uploadsDir = "uploads/";
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadsDir + fileName);

            try {
                Files.createDirectories(Paths.get(uploadsDir));
                file.transferTo(path);
                String imageUrl = "/uploads/" + fileName;
                userProfile.setProfileImageUrl(imageUrl);
            } catch (Exception e) {
                log.error("Error uploading profile image: ", e);
                userProfile.setProfileImageUrl(existingProfile.getProfileImageUrl());
            }
        } else {
            // Keep existing image if no new upload
            userProfile.setProfileImageUrl(existingProfile.getProfileImageUrl());
        }

        // Keep existing dateOfBirth if not changed
        if (userProfile.getDateOfBirth() == null) {
            userProfile.setDateOfBirth(existingProfile.getDateOfBirth());
        }

        userProfileService.updateUserProfile(currentPrincipalName, userProfile);

        // Refresh user information for header
        UserProfileResponse updatedProfile = userProfileService.getUserProfile(currentPrincipalName);
        model.addAttribute("userProfile", updatedProfile);

        return "redirect:/admin/profile";
    }

    @PostMapping("/settings/update")
    public String updateSystemSettings(@ModelAttribute SystemConfiguration config, 
                                     RedirectAttributes redirectAttributes) {
        try {
            // In a real application, you would save this to a database or configuration file
            log.info("System settings updated: {}", config);
            redirectAttributes.addFlashAttribute("successMessage", 
                "System settings updated successfully");
        } catch (Exception e) {
            log.error("Error updating system settings: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error updating settings: " + e.getMessage());
        }
        
        return "redirect:/admin/settings";
    }

    @GetMapping("/logs/export/{format}")
    public ResponseEntity<Resource> exportLogs(@PathVariable String format) {
        try {
            // Get system logs
            List<SystemLog> logs = getSystemLogs();
            
            // Create export file
            String fileName = "system_logs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
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
            
            // Create resource
            ByteArrayResource resource = new ByteArrayResource(content.getBytes());
            
            // Return as downloadable file
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
            
        } catch (Exception e) {
            log.error("Error exporting logs: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private String generateLogsTxt(List<SystemLog> logs) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("SYSTEM LOGS EXPORT\n");
        sb.append("=================\n\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (SystemLog log : logs) {
            sb.append(log.getTimestamp().format(formatter))
              .append(" [").append(log.getLevel()).append("] ")
              .append(log.getSource()).append(" - ")
              .append(log.getMessage())
              .append("\n");
        }
        
        return sb.toString();
    }
    
    private String generateLogsCsv(List<SystemLog> logs) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("Timestamp,Level,Source,Message\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Data rows
        for (SystemLog log : logs) {
            sb.append(log.getTimestamp().format(formatter)).append(",")
              .append(log.getLevel()).append(",")
              .append(escapeCsvField(log.getSource())).append(",")
              .append(escapeCsvField(log.getMessage()))
              .append("\n");
        }
        
        return sb.toString();
    }
    
    private String escapeCsvField(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
    
    private String generateLogsJson(List<SystemLog> logs) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(logs);
        } catch (Exception e) {
            log.error("Error generating JSON logs: ", e);
            return "[]";
        }
    }

    // Helper methods for admin-specific statistics and data

    private AdminDashboardStats getAdminDashboardStatistics() {
        try {
            List<User> allUsers = userRepository.findAll();
            List<MedicalServiceResponse> services = medicalServiceManageService.getAllServices();
            List<ServiceOrderResponse> orders = orderTaskManagementService.getServiceOrders();

            long totalUsers = allUsers.size();
            long activeUsers = allUsers.stream()
                    .filter(User::getIsActive)
                    .count();
            long totalServices = services.size();
            long totalOrders = orders.size();
            long pendingOrders = orders.stream()
                    .filter(order -> order.getOrderStatus().name().equals("PENDING"))
                    .count();

            // Calculate revenue (simplified)
            double totalRevenue = orders.stream()
                    .filter(order -> order.getOrderStatus().name().equals("COMPLETED"))
                    .mapToDouble(order -> 299.0) // Default price, should be from service
                    .sum();

            return new AdminDashboardStats(totalUsers, activeUsers, totalServices, 
                    totalOrders, pendingOrders, totalRevenue);

        } catch (Exception e) {
            log.error("Error getting admin dashboard statistics: ", e);
            return new AdminDashboardStats(0, 0, 0, 0, 0, 0.0);
        }
    }

    private List<SystemActivity> getRecentSystemActivities() {
        List<SystemActivity> activities = new ArrayList<>();

        try {
            // Get recent orders and convert to activities
            List<ServiceOrderResponse> recentOrders = orderTaskManagementService.getServiceOrders()
                    .stream()
                    .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                    .limit(10)
                    .collect(Collectors.toList());

            for (ServiceOrderResponse order : recentOrders) {
                String timeAgo = getTimeAgo(order.getCreatedAt());
                activities.add(new SystemActivity(
                        "ORDER",
                        "New Order Created",
                        "Order #" + order.getId() + " created by " + order.getCustomerName(),
                        timeAgo,
                        "success"
                ));
            }

            // Add other system activities (user registrations, service updates, etc.)
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
                        "info"
                ));
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
            // Check for system issues
            List<ServiceOrderResponse> orders = orderTaskManagementService.getServiceOrders();
            
            // Alert for pending orders
            long pendingOrders = orders.stream()
                    .filter(order -> order.getOrderStatus().name().equals("PENDING"))
                    .count();
            
            if (pendingOrders > 10) {
                alerts.add(new SystemAlert(
                        "warning",
                        "High Pending Orders",
                        pendingOrders + " orders are pending processing",
                        "high"
                ));
            }

            // Alert for inactive users
            List<User> allUsers = userRepository.findAll();
            long inactiveUsers = allUsers.stream()
                    .filter(user -> !user.getIsActive())
                    .count();
            
            if (inactiveUsers > 5) {
                alerts.add(new SystemAlert(
                        "info",
                        "Inactive Users",
                        inactiveUsers + " users are currently inactive",
                        "medium"
                ));
            }

            // System health alert (simulated)
            alerts.add(new SystemAlert(
                    "success",
                    "System Running Normally",
                    "All services are operational",
                    "low"
            ));

        } catch (Exception e) {
            log.error("Error getting system alerts: ", e);
            alerts.add(new SystemAlert(
                    "error",
                    "System Error",
                    "Unable to retrieve system status",
                    "critical"
            ));
        }

        return alerts;
    }

    private MonthlySystemStats getMonthlySystemStatistics() {
        try {
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            
            List<ServiceOrderResponse> orders = orderTaskManagementService.getServiceOrders();
            List<ServiceOrderResponse> monthlyOrders = orders.stream()
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
        // Simulated performance metrics
        return new SystemPerformanceMetrics(
                95.5, // CPU usage percentage
                68.2, // Memory usage percentage
                42.1, // Disk usage percentage
                99.9, // System uptime percentage
                350   // Active sessions
        );
    }

    private UserManagementStats getUserManagementStatistics() {
        try {
            List<User> allUsers = userRepository.findAll();
            
            long totalUsers = allUsers.size();
            long activeUsers = allUsers.stream().filter(User::getIsActive).count();
            long inactiveUsers = totalUsers - activeUsers;
            
            // Get role distribution
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

    private List<UserActivity> getRecentUserActivities() {
        List<UserActivity> activities = new ArrayList<>();
        
        try {
            // Simulated user activities
            List<User> recentUsers = userRepository.findAll().stream()
                    .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                    .limit(10)
                    .collect(Collectors.toList());

            for (User user : recentUsers) {
                activities.add(new UserActivity(
                        user.getUsername(),
                        "Last Login",
                        getTimeAgo(user.getCreatedAt()),
                        user.getIsActive() ? "Online" : "Offline"
                ));
            }

        } catch (Exception e) {
            log.error("Error getting recent user activities: ", e);
        }
        
        return activities;
    }

    private SystemConfiguration getSystemConfiguration() {
        // Simulated system configuration
        return new SystemConfiguration(
                "Bloodline DNA Testing System",
                "1.0.0",
                "admin@bloodline.com",
                "Production",
                true,
                true,
                "smtp.bloodline.com"
        );
    }

    private List<SystemLog> getSystemLogs() {
        List<SystemLog> logs = new ArrayList<>();
        
        // Simulated system logs
        logs.add(new SystemLog("INFO", "System startup completed", "System", LocalDateTime.now().minusHours(2)));
        logs.add(new SystemLog("WARN", "High memory usage detected", "Performance", LocalDateTime.now().minusMinutes(30)));
        logs.add(new SystemLog("ERROR", "Database connection timeout", "Database", LocalDateTime.now().minusMinutes(15)));
        logs.add(new SystemLog("INFO", "User authentication successful", "Security", LocalDateTime.now().minusMinutes(5)));
        
        return logs;
    }

    private LogStatistics getLogStatistics() {
        return new LogStatistics(156, 23, 8, 2);
    }

    private SystemAnalytics getSystemAnalytics() {
        return new SystemAnalytics(
                1250,  // Total page views
                89.5,  // Conversion rate
                156,   // Active sessions
                34.2   // Bounce rate
        );
    }

    private List<PerformanceTrend> getPerformanceTrends() {
        List<PerformanceTrend> trends = new ArrayList<>();
        
        // Simulated performance trends for the last 7 days
        for (int i = 6; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            trends.add(new PerformanceTrend(
                    date.format(DateTimeFormatter.ofPattern("MM/dd")),
                    95.0 + (Math.random() * 5), // Response time
                    Math.random() * 100,        // CPU usage
                    Math.random() * 100         // Memory usage
            ));
        }
        
        return trends;
    }

    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) return hours + " hour" + (hours == 1 ? "" : "s") + " ago";

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 7) return days + " day" + (days == 1 ? "" : "s") + " ago";

        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // Helper methods for analytics
    private double calculateTotalRevenue(LocalDate startDate, LocalDate endDate) {
        // In a real implementation, this would query a repository
        // For now, return a sample value
        return 157890.50;
    }
    
    private int calculateTotalOrders(LocalDate startDate, LocalDate endDate) {
        // In a real implementation, this would query a repository
        // For now, return a sample value
        return 1245;
    }
    
    private String convertToChartData(RevenueData data) {
        try {
            Map<String, Object> chartData = new HashMap<>();
            chartData.put("labels", data.getLabels());
            
            Map<String, Object> dataset = new HashMap<>();
            dataset.put("label", "Revenue");
            dataset.put("data", data.getValues());
            dataset.put("borderColor", "rgb(75, 192, 192)");
            dataset.put("backgroundColor", "rgba(75, 192, 192, 0.1)");
            dataset.put("tension", 0.4);
            dataset.put("fill", true);
            
            List<Map<String, Object>> datasets = new ArrayList<>();
            datasets.add(dataset);
            
            chartData.put("datasets", datasets);
            
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(chartData);
        } catch (Exception e) {
            log.error("Error converting revenue data to chart format", e);
            return "{}";
        }
    }
    
    private String convertToChartData(OrderStatusData data) {
        try {
            Map<String, Object> chartData = new HashMap<>();
            chartData.put("labels", data.getLabels());
            
            Map<String, Object> dataset = new HashMap<>();
            dataset.put("data", data.getValues());
            dataset.put("backgroundColor", data.getColors());
            
            List<Map<String, Object>> datasets = new ArrayList<>();
            datasets.add(dataset);
            
            chartData.put("datasets", datasets);
            
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(chartData);
        } catch (Exception e) {
            log.error("Error converting order status data to chart format", e);
            return "{}";
        }
    }
    
    private String convertToChartData(UserActivityData data) {
        try {
            Map<String, Object> chartData = new HashMap<>();
            chartData.put("labels", data.getLabels());
            
            List<Map<String, Object>> datasets = new ArrayList<>();
            
            // Registrations dataset
            Map<String, Object> registrationsDataset = new HashMap<>();
            registrationsDataset.put("label", "New Registrations");
            registrationsDataset.put("data", data.getRegistrations());
            registrationsDataset.put("backgroundColor", "rgba(255, 99, 132, 0.8)");
            datasets.add(registrationsDataset);
            
            // Logins dataset
            Map<String, Object> loginsDataset = new HashMap<>();
            loginsDataset.put("label", "Logins");
            loginsDataset.put("data", data.getLogins());
            loginsDataset.put("backgroundColor", "rgba(54, 162, 235, 0.8)");
            datasets.add(loginsDataset);
            
            // Active users dataset
            Map<String, Object> activeUsersDataset = new HashMap<>();
            activeUsersDataset.put("label", "Active Users");
            activeUsersDataset.put("data", data.getActiveUsers());
            activeUsersDataset.put("backgroundColor", "rgba(75, 192, 192, 0.8)");
            datasets.add(activeUsersDataset);
            
            chartData.put("datasets", datasets);
            
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(chartData);
        } catch (Exception e) {
            log.error("Error converting user activity data to chart format", e);
            return "{}";
        }
    }
    
    private String convertToChartData(ServicePerformanceData data) {
        try {
            Map<String, Object> chartData = new HashMap<>();
            chartData.put("labels", data.getLabels());
            
            List<Map<String, Object>> datasets = new ArrayList<>();
            
            // Orders dataset
            Map<String, Object> ordersDataset = new HashMap<>();
            ordersDataset.put("label", "Orders");
            ordersDataset.put("data", data.getOrders());
            ordersDataset.put("backgroundColor", "rgba(255, 159, 64, 0.8)");
            datasets.add(ordersDataset);
            
            // Completions dataset
            Map<String, Object> completionsDataset = new HashMap<>();
            completionsDataset.put("label", "Completions");
            completionsDataset.put("data", data.getCompletions());
            completionsDataset.put("backgroundColor", "rgba(75, 192, 192, 0.8)");
            datasets.add(completionsDataset);
            
            chartData.put("datasets", datasets);
            
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(chartData);
        } catch (Exception e) {
            log.error("Error converting service performance data to chart format", e);
            return "{}";
        }
    }

    // Inner classes for admin-specific data structures

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

        // Getters
        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getTotalServices() { return totalServices; }
        public long getTotalOrders() { return totalOrders; }
        public long getPendingOrders() { return pendingOrders; }
        public double getTotalRevenue() { return totalRevenue; }
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

        // Getters
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getTimeAgo() { return timeAgo; }
        public String getSeverity() { return severity; }
        public LocalDateTime getTimestamp() { return timestamp; }
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

        // Getters
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getPriority() { return priority; }
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

        // Getters
        public long getNewOrders() { return newOrders; }
        public long getNewUsers() { return newUsers; }
        public double getRevenue() { return revenue; }
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

        // Getters
        public double getCpuUsage() { return cpuUsage; }
        public double getMemoryUsage() { return memoryUsage; }
        public double getDiskUsage() { return diskUsage; }
        public double getUptime() { return uptime; }
        public int getActiveSessions() { return activeSessions; }
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

        // Getters
        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getInactiveUsers() { return inactiveUsers; }
        public long getAdminUsers() { return adminUsers; }
        public long getManagerUsers() { return managerUsers; }
        public long getStaffUsers() { return staffUsers; }
        public long getCustomerUsers() { return customerUsers; }
    }

    public static class UserActivity {
        private final String username;
        private final String activity;
        private final String timeAgo;
        private final String status;

        public UserActivity(String username, String activity, String timeAgo, String status) {
            this.username = username;
            this.activity = activity;
            this.timeAgo = timeAgo;
            this.status = status;
        }

        // Getters
        public String getUsername() { return username; }
        public String getActivity() { return activity; }
        public String getTimeAgo() { return timeAgo; }
        public String getStatus() { return status; }
    }

    public static class SystemConfiguration {
        private final String systemName;
        private final String version;
        private final String adminEmail;
        private final String environment;
        private final boolean maintenanceMode;
        private final boolean emailNotifications;
        private final String smtpServer;

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

        // Getters
        public String getSystemName() { return systemName; }
        public String getVersion() { return version; }
        public String getAdminEmail() { return adminEmail; }
        public String getEnvironment() { return environment; }
        public boolean isMaintenanceMode() { return maintenanceMode; }
        public boolean isEmailNotifications() { return emailNotifications; }
        public String getSmtpServer() { return smtpServer; }
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

        // Getters
        public String getLevel() { return level; }
        public String getMessage() { return message; }
        public String getSource() { return source; }
        public LocalDateTime getTimestamp() { return timestamp; }
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

        // Getters
        public long getInfoLogs() { return infoLogs; }
        public long getWarningLogs() { return warningLogs; }
        public long getErrorLogs() { return errorLogs; }
        public long getCriticalLogs() { return criticalLogs; }
    }

    public static class SystemAnalytics {
        private final long pageViews;
        private final double conversionRate;
        private final long activeSessions;
        private final double bounceRate;

        public SystemAnalytics(long pageViews, double conversionRate, long activeSessions, double bounceRate) {
            this.pageViews = pageViews;
            this.conversionRate = conversionRate;
            this.activeSessions = activeSessions;
            this.bounceRate = bounceRate;
        }

        // Getters
        public long getPageViews() { return pageViews; }
        public double getConversionRate() { return conversionRate; }
        public long getActiveSessions() { return activeSessions; }
        public double getBounceRate() { return bounceRate; }
    }

    public static class PerformanceTrend {
        private final String date;
        private final double responseTime;
        private final double cpuUsage;
        private final double memoryUsage;

        public PerformanceTrend(String date, double responseTime, double cpuUsage, double memoryUsage) {
            this.date = date;
            this.responseTime = responseTime;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
        }

        // Getters
        public String getDate() { return date; }
        public double getResponseTime() { return responseTime; }
        public double getCpuUsage() { return cpuUsage; }
        public double getMemoryUsage() { return memoryUsage; }
    }

    // Analytics Data Classes
    public static class AnalyticsSummary {
        private String totalRevenue;
        private String totalOrders;
        private String activeUsers;
        private String avgOrderValue;
        private String revenueGrowth;
        private String ordersGrowth;
        private String usersGrowth;
        private String aovGrowth;

        public AnalyticsSummary() {
        }

        // Getters and Setters
        public String getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(String totalRevenue) { this.totalRevenue = totalRevenue; }
        
        public String getTotalOrders() { return totalOrders; }
        public void setTotalOrders(String totalOrders) { this.totalOrders = totalOrders; }
        
        public String getActiveUsers() { return activeUsers; }
        public void setActiveUsers(String activeUsers) { this.activeUsers = activeUsers; }
        
        public String getAvgOrderValue() { return avgOrderValue; }
        public void setAvgOrderValue(String avgOrderValue) { this.avgOrderValue = avgOrderValue; }
        
        public String getRevenueGrowth() { return revenueGrowth; }
        public void setRevenueGrowth(String revenueGrowth) { this.revenueGrowth = revenueGrowth; }
        
        public String getOrdersGrowth() { return ordersGrowth; }
        public void setOrdersGrowth(String ordersGrowth) { this.ordersGrowth = ordersGrowth; }
        
        public String getUsersGrowth() { return usersGrowth; }
        public void setUsersGrowth(String usersGrowth) { this.usersGrowth = usersGrowth; }
        
        public String getAovGrowth() { return aovGrowth; }
        public void setAovGrowth(String aovGrowth) { this.aovGrowth = aovGrowth; }
    }

    public static class RevenueData {
        private List<String> labels;
        private List<Double> values;

        public RevenueData(List<String> labels, List<Double> values) {
            this.labels = labels;
            this.values = values;
        }

        // Getters
        public List<String> getLabels() { return labels; }
        public List<Double> getValues() { return values; }
    }

    public static class OrderStatusData {
        private List<String> labels;
        private List<Integer> values;
        private List<String> colors;

        public OrderStatusData(List<String> labels, List<Integer> values, List<String> colors) {
            this.labels = labels;
            this.values = values;
            this.colors = colors;
        }

        // Getters
        public List<String> getLabels() { return labels; }
        public List<Integer> getValues() { return values; }
        public List<String> getColors() { return colors; }
    }

    public static class UserActivityData {
        private List<String> labels;
        private List<Integer> registrations;
        private List<Integer> logins;
        private List<Integer> activeUsers;

        public UserActivityData(List<String> labels, List<Integer> registrations, 
                               List<Integer> logins, List<Integer> activeUsers) {
            this.labels = labels;
            this.registrations = registrations;
            this.logins = logins;
            this.activeUsers = activeUsers;
        }

        // Getters
        public List<String> getLabels() { return labels; }
        public List<Integer> getRegistrations() { return registrations; }
        public List<Integer> getLogins() { return logins; }
        public List<Integer> getActiveUsers() { return activeUsers; }
    }

    public static class ServicePerformanceData {
        private List<String> labels;
        private List<Integer> orders;
        private List<Integer> completions;
        private List<Double> avgCompletionTime;

        public ServicePerformanceData(List<String> labels, List<Integer> orders,
                                     List<Integer> completions, List<Double> avgCompletionTime) {
            this.labels = labels;
            this.orders = orders;
            this.completions = completions;
            this.avgCompletionTime = avgCompletionTime;
        }

        // Getters
        public List<String> getLabels() { return labels; }
        public List<Integer> getOrders() { return orders; }
        public List<Integer> getCompletions() { return completions; }
        public List<Double> getAvgCompletionTime() { return avgCompletionTime; }
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

        // Getters
        public double getServerUptime() { return serverUptime; }
        public double getResponseTime() { return responseTime; }
        public double getErrorRate() { return errorRate; }
        public int getDatabaseConnections() { return databaseConnections; }
        public int getActiveSessionCount() { return activeSessionCount; }
    }

    public static class TopServiceData {
        private String serviceName;
        private int orderCount;
        private double revenue;
        private double growthRate;

        public TopServiceData(String serviceName, int orderCount, double revenue, double growthRate) {
            this.serviceName = serviceName;
            this.orderCount = orderCount;
            this.revenue = revenue;
            this.growthRate = growthRate;
        }

        // Getters
        public String getServiceName() { return serviceName; }
        public int getOrderCount() { return orderCount; }
        public double getRevenue() { return revenue; }
        public double getGrowthRate() { return growthRate; }
    }

    public static class GeographicData {
        private String region;
        private int orderCount;
        private double revenue;
        private double marketShare;

        public GeographicData(String region, int orderCount, double revenue, double marketShare) {
            this.region = region;
            this.orderCount = orderCount;
            this.revenue = revenue;
            this.marketShare = marketShare;
        }

        // Getters
        public String getRegion() { return region; }
        public int getOrderCount() { return orderCount; }
        public double getRevenue() { return revenue; }
        public double getMarketShare() { return marketShare; }
    }

    // Analytics data generation methods
    private RevenueData generateRevenueData(LocalDate startDate, LocalDate endDate, String period) {
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        
        // Generate labels and placeholder data based on period
        if ("year".equals(period)) {
            // Monthly data for the year
            for (Month month : Month.values()) {
                labels.add(month.toString().substring(0, 3));
                // Generate random revenue data between 10000 and 50000
                values.add(10000.0 + Math.random() * 40000.0);
            }
        } else if ("month".equals(period)) {
            // Daily data for the month
            int daysInMonth = endDate.getDayOfMonth();
            for (int i = 1; i <= daysInMonth; i++) {
                labels.add(String.valueOf(i));
                // Generate random revenue data between 1000 and 5000
                values.add(1000.0 + Math.random() * 4000.0);
            }
        } else if ("week".equals(period)) {
            // Daily data for the week
            String[] weekDays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (String day : weekDays) {
                labels.add(day);
                // Generate random revenue data between 500 and 2000
                values.add(500.0 + Math.random() * 1500.0);
            }
        } else {
            // Hourly data for today
            for (int i = 0; i < 24; i++) {
                labels.add(i + ":00");
                // Generate random revenue data between 100 and 500
                values.add(100.0 + Math.random() * 400.0);
            }
        }
        
        return new RevenueData(labels, values);
    }
    
    private OrderStatusData generateOrderStatusData(LocalDate startDate, LocalDate endDate) {
        List<String> labels = List.of("Completed", "Processing", "Pending", "Cancelled");
        List<Integer> values = List.of(
            (int)(Math.random() * 100) + 50,  // Completed
            (int)(Math.random() * 50) + 20,   // Processing
            (int)(Math.random() * 30) + 10,   // Pending
            (int)(Math.random() * 20) + 5     // Cancelled
        );
        List<String> colors = List.of(
            "#28a745",  // Green for Completed
            "#17a2b8",  // Blue for Processing
            "#ffc107",  // Yellow for Pending
            "#dc3545"   // Red for Cancelled
        );
        
        return new OrderStatusData(labels, values, colors);
    }
    
    private UserActivityData generateUserActivityData(LocalDate startDate, LocalDate endDate, String period) {
        List<String> labels = new ArrayList<>();
        List<Integer> registrations = new ArrayList<>();
        List<Integer> logins = new ArrayList<>();
        List<Integer> activeUsers = new ArrayList<>();
        
        // Generate labels and placeholder data based on period
        if ("year".equals(period)) {
            // Monthly data for the year
            for (Month month : Month.values()) {
                labels.add(month.toString().substring(0, 3));
                registrations.add((int)(Math.random() * 100) + 20);
                logins.add((int)(Math.random() * 500) + 100);
                activeUsers.add((int)(Math.random() * 300) + 200);
            }
        } else if ("month".equals(period)) {
            // Weekly data for the month
            int weeksInMonth = 4;
            for (int i = 1; i <= weeksInMonth; i++) {
                labels.add("Week " + i);
                registrations.add((int)(Math.random() * 50) + 10);
                logins.add((int)(Math.random() * 200) + 50);
                activeUsers.add((int)(Math.random() * 150) + 100);
            }
        } else if ("week".equals(period)) {
            // Daily data for the week
            String[] weekDays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (String day : weekDays) {
                labels.add(day);
                registrations.add((int)(Math.random() * 20) + 5);
                logins.add((int)(Math.random() * 100) + 30);
                activeUsers.add((int)(Math.random() * 80) + 50);
            }
        } else {
            // Hourly data for today
            for (int i = 0; i < 24; i += 3) {
                labels.add(i + ":00");
                registrations.add((int)(Math.random() * 10) + 1);
                logins.add((int)(Math.random() * 50) + 10);
                activeUsers.add((int)(Math.random() * 40) + 20);
            }
        }
        
        return new UserActivityData(labels, registrations, logins, activeUsers);
    }
    
    private ServicePerformanceData generateServicePerformanceData(LocalDate startDate, LocalDate endDate) {
        // Sample service types
        List<String> labels = List.of(
            "DNA Paternity",
            "DNA Sibling",
            "DNA Ancestry",
            "DNA Health",
            "DNA Forensic"
        );
        
        List<Integer> orders = new ArrayList<>();
        List<Integer> completions = new ArrayList<>();
        List<Double> avgCompletionTime = new ArrayList<>();
        
        // Generate random data for each service
        for (int i = 0; i < labels.size(); i++) {
            int orderCount = (int)(Math.random() * 100) + 20;
            orders.add(orderCount);
            
            // Completions should be less than or equal to orders
            int completionCount = (int)(orderCount * (0.7 + Math.random() * 0.3));
            completions.add(completionCount);
            
            // Average completion time in days (1-10 days)
            avgCompletionTime.add(1.0 + Math.random() * 9.0);
        }
        
        return new ServicePerformanceData(labels, orders, completions, avgCompletionTime);
    }
    
    private List<TopServiceData> generateTopServicesData(LocalDate startDate, LocalDate endDate) {
        List<TopServiceData> topServices = new ArrayList<>();
        
        // Sample service names
        String[] serviceNames = {
            "DNA Paternity Testing",
            "DNA Sibling Testing",
            "DNA Ancestry Analysis",
            "DNA Health Screening",
            "DNA Forensic Testing"
        };
        
        // Generate sample data for each service
        for (String serviceName : serviceNames) {
            int orderCount = (int)(Math.random() * 200) + 50;
            double revenue = (orderCount * (100 + Math.random() * 400));
            double growthRate = (Math.random() * 30) - 5;  // -5% to 25%
            
            topServices.add(new TopServiceData(serviceName, orderCount, revenue, growthRate));
        }
        
        // Sort by revenue (highest first)
        topServices.sort((a, b) -> Double.compare(b.getRevenue(), a.getRevenue()));
        
        return topServices;
    }
    
    private List<GeographicData> generateGeographicData(LocalDate startDate, LocalDate endDate) {
        List<GeographicData> geographicData = new ArrayList<>();
        
        // Sample regions
        String[] regions = {
            "North Region",
            "South Region",
            "East Region",
            "West Region",
            "Central Region"
        };
        
        int totalOrders = 0;
        double totalRevenue = 0;
        
        // First pass to generate orders and revenue
        Map<String, Integer> ordersByRegion = new HashMap<>();
        Map<String, Double> revenueByRegion = new HashMap<>();
        
        for (String region : regions) {
            int orderCount = (int)(Math.random() * 200) + 50;
            double revenue = (orderCount * (100 + Math.random() * 400));
            
            ordersByRegion.put(region, orderCount);
            revenueByRegion.put(region, revenue);
            
            totalOrders += orderCount;
            totalRevenue += revenue;
        }
        
        // Second pass to calculate market share and create objects
        for (String region : regions) {
            int orderCount = ordersByRegion.get(region);
            double revenue = revenueByRegion.get(region);
            double marketShare = (revenue / totalRevenue) * 100;
            
            geographicData.add(new GeographicData(region, orderCount, revenue, marketShare));
        }
        
        // Sort by revenue (highest first)
        geographicData.sort((a, b) -> Double.compare(b.getRevenue(), a.getRevenue()));
        
        return geographicData;
    }
    
    private SystemMetricsData generateSystemMetricsData() {
        // Generate random system metrics
        double serverUptime = 99.5 + (Math.random() * 0.5);  // 99.5% to 100%
        double responseTime = 50 + (Math.random() * 150);    // 50ms to 200ms
        double errorRate = Math.random() * 1.0;              // 0% to 1%
        int databaseConnections = (int)(Math.random() * 50) + 10;
        int activeSessionCount = (int)(Math.random() * 100) + 20;
        
        return new SystemMetricsData(serverUptime, responseTime, errorRate, 
                                   databaseConnections, activeSessionCount);
    }

    @GetMapping("/orders")
    public String orders(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "all") String status,
                        @RequestParam(defaultValue = "all") String paymentStatus,
                        @RequestParam(defaultValue = "") String search,
                        @RequestParam(defaultValue = "createdAt") String sortBy,
                        @RequestParam(defaultValue = "desc") String sortDir,
                        Model model) {
        addUserProfileToModel(model);
        model.addAttribute("pageTitle", "Orders Overview");
        
        // Add breadcrumbs
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(new BreadcrumbItem("Orders Overview", ""));
        model.addAttribute("breadcrumbs", breadcrumbs);

        try {
            // Get all orders with filtering and pagination
            List<ServiceOrderResponse> allOrders = orderTaskManagementService.getServiceOrders();
            
            // Apply filters
            List<ServiceOrderResponse> filteredOrders = allOrders.stream()
                .filter(order -> {
                    boolean matchesStatus = "all".equals(status) || 
                        order.getOrderStatus().name().toLowerCase().equals(status.toLowerCase());
                    boolean matchesPaymentStatus = "all".equals(paymentStatus) || 
                        order.getPaymentStatus().name().toLowerCase().equals(paymentStatus.toLowerCase());
                    boolean matchesSearch = search.isEmpty() || 
                        order.getCustomerName().toLowerCase().contains(search.toLowerCase()) ||
                        order.getServiceName().toLowerCase().contains(search.toLowerCase()) ||
                        order.getId().toString().contains(search);
                    return matchesStatus && matchesPaymentStatus && matchesSearch;
                })
                .sorted((o1, o2) -> {
                    int result = 0;
                    switch (sortBy) {
                        case "customerName":
                            result = o1.getCustomerName().compareTo(o2.getCustomerName());
                            break;
                        case "serviceName":
                            result = o1.getServiceName().compareTo(o2.getServiceName());
                            break;
                        case "totalAmount":
                            result = o1.getTotalAmount().compareTo(o2.getTotalAmount());
                            break;
                        case "orderStatus":
                            result = o1.getOrderStatus().compareTo(o2.getOrderStatus());
                            break;
                        case "paymentStatus":
                            result = o1.getPaymentStatus().compareTo(o2.getPaymentStatus());
                            break;
                        case "createdAt":
                        default:
                            result = o1.getCreatedAt().compareTo(o2.getCreatedAt());
                            break;
                    }
                    return "desc".equals(sortDir) ? -result : result;
                })
                .collect(Collectors.toList());

            // Calculate pagination
            int start = page * size;
            int end = Math.min(start + size, filteredOrders.size());
            List<ServiceOrderResponse> paginatedOrders = start < filteredOrders.size() ? 
                filteredOrders.subList(start, end) : new ArrayList<>();

            // Calculate order statistics
            OrderStatistics orderStats = calculateOrderStatistics(allOrders);
            
            // Add attributes to model
            model.addAttribute("orders", paginatedOrders);
            model.addAttribute("totalOrders", filteredOrders.size());
            model.addAttribute("totalPages", (int) Math.ceil((double) filteredOrders.size() / size));
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("selectedPaymentStatus", paymentStatus);
            model.addAttribute("searchQuery", search);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("orderStats", orderStats);

        } catch (Exception e) {
            log.error("Error loading orders overview: ", e);
            model.addAttribute("error", "Unable to load orders data");
            model.addAttribute("orders", new ArrayList<>());
            model.addAttribute("totalOrders", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("currentPage", 0);
        }

        return "admin/orders";
    }

    @GetMapping("/orders/{orderId}")
    public String orderDetails(@PathVariable Long orderId, Model model) {
        addUserProfileToModel(model);
        model.addAttribute("pageTitle", "Order Details");
        
        // Add breadcrumbs
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(new BreadcrumbItem("Orders", "/admin/orders"));
        breadcrumbs.add(new BreadcrumbItem("Order #" + orderId, ""));
        model.addAttribute("breadcrumbs", breadcrumbs);

        try {
            // Get order details
            List<ServiceOrderResponse> allOrders = orderTaskManagementService.getServiceOrders();
            ServiceOrderResponse order = allOrders.stream()
                .filter(o -> o.getId().equals(orderId))
                .findFirst()
                .orElse(null);

            if (order == null) {
                model.addAttribute("error", "Order not found");
                return "admin/orders";
            }

            model.addAttribute("order", order);

        } catch (Exception e) {
            log.error("Error loading order details for order ID: " + orderId, e);
            model.addAttribute("error", "Unable to load order details");
        }

        return "admin/order-details";
    }

    @PostMapping("/orders/{orderId}/update-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            @RequestParam(required = false) String notes) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            orderTaskManagementService.updateOrderStatus(orderId, status);
            response.put("success", true);
            response.put("message", "Order status updated successfully");
        } catch (Exception e) {
            log.error("Error updating order status: ", e);
            response.put("success", false);
            response.put("message", "Failed to update order status: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    private OrderStatistics calculateOrderStatistics(List<ServiceOrderResponse> orders) {
        OrderStatistics stats = new OrderStatistics();
        
        stats.setTotalOrders(orders.size());
        stats.setPendingOrders((int) orders.stream()
            .filter(o -> "PENDING".equals(o.getOrderStatus().name())).count());
        stats.setConfirmedOrders((int) orders.stream()
            .filter(o -> "CONFIRMED".equals(o.getOrderStatus().name())).count());
        stats.setInProgressOrders((int) orders.stream()
            .filter(o -> "IN_PROGRESS".equals(o.getOrderStatus().name())).count());
        stats.setCompletedOrders((int) orders.stream()
            .filter(o -> "COMPLETED".equals(o.getOrderStatus().name())).count());
        stats.setCancelledOrders((int) orders.stream()
            .filter(o -> "CANCELLED".equals(o.getOrderStatus().name())).count());
        
        stats.setPaidOrders((int) orders.stream()
            .filter(o -> "PAID".equals(o.getPaymentStatus() != null ? o.getPaymentStatus().name() : null)).count());
        stats.setPendingPaymentOrders((int) orders.stream()
            .filter(o -> "PENDING".equals(o.getPaymentStatus() != null ? o.getPaymentStatus().name() : null)).count());
        
        double totalRevenue = orders.stream()
            .filter(o -> "PAID".equals(o.getPaymentStatus() != null ? o.getPaymentStatus().name() : null))
            .mapToDouble(o -> o.getFinalAmount().doubleValue())
            .sum();
        stats.setTotalRevenue(totalRevenue);
        
        double avgOrderValue = stats.getPaidOrders() > 0 ? totalRevenue / stats.getPaidOrders() : 0;
        stats.setAverageOrderValue(avgOrderValue);
        
        return stats;
    }

    // Helper classes for order statistics
    public static class OrderStatistics {
        private int totalOrders;
        private int pendingOrders;
        private int confirmedOrders;
        private int inProgressOrders;
        private int completedOrders;
        private int cancelledOrders;
        private int paidOrders;
        private int pendingPaymentOrders;
        private double totalRevenue;
        private double averageOrderValue;

        // Getters and setters
        public int getTotalOrders() { return totalOrders; }
        public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
        
        public int getPendingOrders() { return pendingOrders; }
        public void setPendingOrders(int pendingOrders) { this.pendingOrders = pendingOrders; }
        
        public int getConfirmedOrders() { return confirmedOrders; }
        public void setConfirmedOrders(int confirmedOrders) { this.confirmedOrders = confirmedOrders; }
        
        public int getInProgressOrders() { return inProgressOrders; }
        public void setInProgressOrders(int inProgressOrders) { this.inProgressOrders = inProgressOrders; }
        
        public int getCompletedOrders() { return completedOrders; }
        public void setCompletedOrders(int completedOrders) { this.completedOrders = completedOrders; }
        
        public int getCancelledOrders() { return cancelledOrders; }
        public void setCancelledOrders(int cancelledOrders) { this.cancelledOrders = cancelledOrders; }
        
        public int getPaidOrders() { return paidOrders; }
        public void setPaidOrders(int paidOrders) { this.paidOrders = paidOrders; }
        
        public int getPendingPaymentOrders() { return pendingPaymentOrders; }
        public void setPendingPaymentOrders(int pendingPaymentOrders) { this.pendingPaymentOrders = pendingPaymentOrders; }
        
        public double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
        
        public double getAverageOrderValue() { return averageOrderValue; }
        public void setAverageOrderValue(double averageOrderValue) { this.averageOrderValue = averageOrderValue; }
    }

    // Helper classes for breadcrumb navigation
    public static class BreadcrumbItem {
        private String label;
        private String url;

        public BreadcrumbItem(String label, String url) {
            this.label = label;
            this.url = url;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
