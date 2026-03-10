package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.MedicalServiceResponse;
import com.dna_testing_system.dev.dto.response.ServiceOrderResponse;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.service.MedicalServiceManageService;
import com.dna_testing_system.dev.service.NotificationService;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import com.dna_testing_system.dev.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/manager")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManagerDashboardController extends BaseController {

    MedicalServiceManageService medicalServiceManageService;
    OrderTaskManagementService orderTaskManagementService;
    NotificationService notificationService;
    UserProfileService userProfileService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        // In a real application, you would add service statistics here
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {

            String currentPrincipalName = authentication.getName();
            UserProfileResponse userProfile = userProfileService.getUserProfile(currentPrincipalName);
            model.addAttribute("userProfile", userProfile);
        }

        try {
            // Get dashboard statistics
            DashboardStats dashboardStats = getDashboardStatistics();
            model.addAttribute("dashboardStats", dashboardStats);

            // Get recent orders (limit to 5 for dashboard)
            List<ServiceOrderResponse> allOrders = orderTaskManagementService.getServiceOrders();
            List<ServiceOrderResponse> recentOrders = allOrders.stream()
                    .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                    .limit(5)
                    .collect(Collectors.toList());
            model.addAttribute("recentOrders", recentOrders);

            // Get recent activities
            List<RecentActivity> recentActivities = getRecentActivities();
            model.addAttribute("recentActivities", recentActivities);

            // Get monthly statistics
            MonthlyStats monthlyStats = getMonthlyStatistics();
            model.addAttribute("monthlyStats", monthlyStats);

            model.addAttribute("pageTitle", "Dashboard");

        } catch (Exception e) {
            log.error("Error loading dashboard data: ", e);
            model.addAttribute("error", "Unable to load dashboard data");
        }
        return "manager/dashboard";
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
        return "manager/services";
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("pageTitle", "Customer Management");
        // In a real application, you would add customer data here
        return "manager/customers";
    }


    @GetMapping("/staff")
    public String staff(Model model) {
        model.addAttribute("pageTitle", "Staff Management");
        // In a real application, you would add staff data here
        return "manager/staff";
    }

    @GetMapping("/profile")  // URL sẽ là /user/profile
    public String getProfile(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        UserProfileResponse userProfile = userProfileService.getUserProfile(currentPrincipalName);
        model.addAttribute("userProfile", userProfile);
        return "user/profile";
    }

    @GetMapping("/profile/update")
    public String showUpdateProfileForm(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        UserProfileResponse userProfileData = userProfileService.getUserProfile(currentPrincipalName);

        // Dữ liệu cũ chỉ có 1 dòng này
        model.addAttribute("userEditProfile", userProfileData);

        model.addAttribute("userProfile", userProfileData);

        return "user/edit-profile"; //
    }
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("userEditProfile") UserProfileRequest userProfile,
                                @RequestParam(value = "file", required = false) MultipartFile file,
                                Model model) { // Thêm Model
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        UserProfileResponse existingProfile = userProfileService.getUserProfile(currentPrincipalName);


        // Xử lý file upload
        if (file != null && !file.getOriginalFilename().equals("")) {
            String uploadsDir = "uploads/";
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadsDir + fileName);

            try {

                Files.createDirectories(Paths.get(uploadsDir));
                file.transferTo(path);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String imageUrl = "/uploads/" + fileName;
            userProfile.setProfileImageUrl(imageUrl);

        } else {
            // Giữ nguyên ảnh cũ nếu không upload ảnh mới
            userProfile.setProfileImageUrl(existingProfile.getProfileImageUrl());
        }

        // Giữ nguyên dateOfBirth nếu không có thay đổi
        if (userProfile.getDateOfBirth() == null) {
            userProfile.setDateOfBirth(existingProfile.getDateOfBirth());
        }


        userProfileService.updateUserProfile(currentPrincipalName, userProfile);

        // Refresh lại thông tin user cho header
        UserProfileResponse updatedProfile = userProfileService.getUserProfile(currentPrincipalName);
        model.addAttribute("userProfile", updatedProfile);

        return "redirect:/manager/profile";
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

        return "manager/analytics";
    }


    private DashboardStats getDashboardStatistics() {
        try {
            List<MedicalServiceResponse> services = medicalServiceManageService.getAllServices();
            List<ServiceOrderResponse> orders = orderTaskManagementService.getServiceOrders();

            long totalServices = services.size();
            long activeOrders = orders.stream()
                    .filter(order -> order.getOrderStatus().name().equals("IN_PROGRESS") ||
                            order.getOrderStatus().name().equals("PENDING"))
                    .count();
            long pendingResults = orders.stream()
                    .filter(order -> order.getOrderStatus().name().equals("PENDING"))
                    .count();

            // You'll need to implement customer counting based on your user management
            long totalCustomers = orders.stream()
                    .map(ServiceOrderResponse::getCustomerName)
                    .distinct()
                    .count();

            return new DashboardStats(totalServices, activeOrders, pendingResults, totalCustomers);

        } catch (Exception e) {
            log.error("Error getting dashboard statistics: ", e);
            return new DashboardStats(0, 0, 0, 0);
        }
    }

    // Helper method to get recent activities
    private List<RecentActivity> getRecentActivities() {
        List<RecentActivity> activities = new ArrayList<>();

        try {
            // Get recent orders and convert to activities
            List<ServiceOrderResponse> recentOrders = orderTaskManagementService.getServiceOrders()
                    .stream()
                    .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                    .limit(10)
                    .collect(Collectors.toList());

            for (ServiceOrderResponse order : recentOrders) {
                String timeAgo = getTimeAgo(order.getCreatedAt());
                activities.add(new RecentActivity(
                        "ORDER",
                        "New order received",
                        "Order #" + order.getId() + " from " + order.getCustomerName(),
                        timeAgo
                ));
            }

        } catch (Exception e) {
            log.error("Error getting recent activities: ", e);
        }

        return activities;
    }

    // Helper method to get monthly statistics
    private MonthlyStats getMonthlyStatistics() {
        try {
            List<ServiceOrderResponse> orders = orderTaskManagementService.getServiceOrders();
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

            List<ServiceOrderResponse> monthlyOrders = orders.stream()
                    .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                    .collect(Collectors.toList());

            long newOrders = monthlyOrders.size();
            long completedOrders = monthlyOrders.stream()
                    .filter(order -> order.getOrderStatus().name().equals("COMPLETED"))
                    .count();

            // Calculate revenue based on your pricing logic
            double revenue = monthlyOrders.stream()
                    .filter(order -> order.getOrderStatus().name().equals("COMPLETED"))
                    .mapToDouble(order -> 299.0) // Default price, you should get this from service
                    .sum();

            return new MonthlyStats(newOrders, completedOrders, revenue);

        } catch (Exception e) {
            log.error("Error getting monthly statistics: ", e);
            return new MonthlyStats(0, 0, 0.0);
        }
    }

    // Helper method to calculate time ago
    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minutes ago";

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) return hours + " hours ago";

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 7) return days + " days ago";

        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // Inner classes for data structure
    public static class DashboardStats {
        private final long totalServices;
        private final long activeOrders;
        private final long pendingResults;
        private final long totalCustomers;

        public DashboardStats(long totalServices, long activeOrders, long pendingResults, long totalCustomers) {
            this.totalServices = totalServices;
            this.activeOrders = activeOrders;
            this.pendingResults = pendingResults;
            this.totalCustomers = totalCustomers;
        }

        // Getters
        public long getTotalServices() { return totalServices; }
        public long getActiveOrders() { return activeOrders; }
        public long getPendingResults() { return pendingResults; }
        public long getTotalCustomers() { return totalCustomers; }
    }

    public static class RecentActivity {
        private final String type;
        private final String title;
        private final String description;
        private final String timeAgo;

        public RecentActivity(String type, String title, String description, String timeAgo) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.timeAgo = timeAgo;
        }

        // Getters
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getTimeAgo() { return timeAgo; }
    }

    public static class MonthlyStats {
        private final long newOrders;
        private final long completedOrders;
        private final double revenue;

        public MonthlyStats(long newOrders, long completedOrders, double revenue) {
            this.newOrders = newOrders;
            this.completedOrders = completedOrders;
            this.revenue = revenue;
        }

        // Getters
        public long getNewOrders() { return newOrders; }
        public long getCompletedOrders() { return completedOrders; }
        public double getRevenue() { return revenue; }
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
    private OrderStatusData generateOrderStatusData(LocalDate startDate, LocalDate endDate) {
        List<String> labels = List.of("Completed", "Processing", "Pending", "Cancelled");
        List<Integer> values = List.of(
                (int)(Math.random() * 100) + 50,  // Completed
                (int)(Math.random() * 50) + 20,   // Processing
                (int)(Math.random() * 30) + 10,   // Pending
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

}
