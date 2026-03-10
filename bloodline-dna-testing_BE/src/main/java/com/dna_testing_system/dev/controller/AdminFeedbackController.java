package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.RespondFeedbackRequest;
import com.dna_testing_system.dev.dto.response.CustomerFeedbackResponse;
import com.dna_testing_system.dev.exception.EntityNotFoundException;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.CustomerFeedbackService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminFeedbackController extends BaseController {

    CustomerFeedbackService customerFeedbackService;
    UserRepository userRepository;

    @GetMapping("/feedback")
    public String feedback(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(defaultValue = "") String customerName,
                          @RequestParam(defaultValue = "") String serviceName,
                          @RequestParam(defaultValue = "all") String responseStatus,
                          @RequestParam(defaultValue = "") String search,
                          @RequestParam(defaultValue = "createdAt") String sortBy,
                          @RequestParam(defaultValue = "desc") String sortDir,
                          Model model) {
        addUserProfileToModel(model);
        model.addAttribute("pageTitle", "Customer Feedback Management");
        
        // Add breadcrumbs
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(new BreadcrumbItem("Customer Feedback", ""));
        model.addAttribute("breadcrumbs", breadcrumbs);

        try {
            // Get all feedback with pagination and filtering
            Page<CustomerFeedbackResponse> feedbackPage = customerFeedbackService.getAllFeedbacks(page, size, search);
            
            // Apply additional filters if needed
            List<CustomerFeedbackResponse> allFeedback = feedbackPage.getContent();
            List<CustomerFeedbackResponse> filteredFeedback = allFeedback.stream()
                .filter(feedback -> feedback != null)
                .filter(feedback -> {
                    boolean matchesCustomer = customerName.isEmpty() || 
                        (feedback.getCustomerName() != null && feedback.getCustomerName().toLowerCase().contains(customerName.toLowerCase()));
                    boolean matchesService = serviceName.isEmpty() || 
                        (feedback.getServiceName() != null && feedback.getServiceName().toLowerCase().contains(serviceName.toLowerCase()));
                    boolean matchesResponseStatus = "all".equals(responseStatus) || 
                        ("responded".equals(responseStatus) && feedback.getRespondedAt() != null) ||
                        ("unresponded".equals(responseStatus) && feedback.getRespondedAt() == null);
                    return matchesCustomer && matchesService && matchesResponseStatus;
                })
                .collect(Collectors.toList());

            // Calculate feedback statistics
            FeedbackStatistics feedbackStats = calculateFeedbackStatistics(allFeedback);
            
            // Add attributes to model
            model.addAttribute("feedbackList", filteredFeedback);
            model.addAttribute("totalFeedback", (int) feedbackPage.getTotalElements());
            model.addAttribute("totalPages", feedbackPage.getTotalPages());
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("selectedCustomerName", customerName);
            model.addAttribute("selectedServiceName", serviceName);
            model.addAttribute("selectedResponseStatus", responseStatus);
            model.addAttribute("searchQuery", search);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("feedbackStats", feedbackStats);

        } catch (Exception e) {
            log.error("Error loading customer feedback: ", e);
            model.addAttribute("error", "Unable to load feedback data: " + e.getMessage());
            model.addAttribute("feedbackList", new ArrayList<>());
            model.addAttribute("totalFeedback", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("currentPage", 0);
            model.addAttribute("feedbackStats", new FeedbackStatistics());
        }

        return "admin/feedback";
    }

    @GetMapping("/feedback/{feedbackId}")
    public String feedbackDetails(@PathVariable Long feedbackId, Model model) {
        addUserProfileToModel(model);
        model.addAttribute("pageTitle", "Feedback Details");
        
        // Add breadcrumbs
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(new BreadcrumbItem("Customer Feedback", "/admin/feedback"));
        breadcrumbs.add(new BreadcrumbItem("Feedback #" + feedbackId, ""));
        model.addAttribute("breadcrumbs", breadcrumbs);

        try {
            CustomerFeedbackResponse feedback = customerFeedbackService.getFeedbackById(feedbackId);
            
            if (feedback == null) {
                model.addAttribute("error", "Feedback not found");
                return "admin/feedback";
            }
            
            model.addAttribute("feedback", feedback);

        } catch (Exception e) {
            log.error("Error loading feedback details for feedback ID: " + feedbackId, e);
            model.addAttribute("error", "Unable to load feedback details");
        }

        return "admin/feedback-details";
    }

    @PostMapping("/feedback/{feedbackId}/respond")
    public String respondToFeedback(@PathVariable Long feedbackId,
                                   @RequestParam String responseContent,
                                   RedirectAttributes redirectAttributes) {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            var existingUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));
            String currentUserId = existingUser.getId();
            
            RespondFeedbackRequest request = RespondFeedbackRequest.builder()
                .respondByUserId(currentUserId)
                .responseContent(responseContent)
                .build();
            
            customerFeedbackService.respondToFeedback(feedbackId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Response submitted successfully");
        } catch (Exception e) {
            log.error("Error responding to feedback: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to submit response: " + e.getMessage());
        }
        
        return "redirect:/admin/feedback/" + feedbackId;
    }

    private FeedbackStatistics calculateFeedbackStatistics(List<CustomerFeedbackResponse> feedbackList) {
        FeedbackStatistics stats = new FeedbackStatistics();
        
        if (feedbackList == null || feedbackList.isEmpty()) {
            return stats;
        }
        
        stats.setTotalFeedback(feedbackList.size());
        stats.setResponseRequiredCount((int) feedbackList.stream()
            .filter(f -> f != null && Boolean.TRUE.equals(f.getResponseRequired())).count());
        stats.setRespondedCount((int) feedbackList.stream()
            .filter(f -> f != null && f.getRespondedAt() != null).count());
        stats.setPendingResponseCount((int) feedbackList.stream()
            .filter(f -> f != null && Boolean.TRUE.equals(f.getResponseRequired()) && f.getRespondedAt() == null).count());
        
        // Calculate average ratings
        double avgOverallRating = feedbackList.stream()
            .filter(f -> f != null && f.getOverallRating() != null)
            .mapToDouble(f -> f.getOverallRating().doubleValue())
            .average()
            .orElse(0.0);
        stats.setAverageOverallRating(avgOverallRating);
        
        double avgServiceQuality = feedbackList.stream()
            .filter(f -> f != null && f.getServiceQualityRating() != null)
            .mapToDouble(f -> f.getServiceQualityRating().doubleValue())
            .average()
            .orElse(0.0);
        stats.setAverageServiceQuality(avgServiceQuality);
        
        double avgStaffBehavior = feedbackList.stream()
            .filter(f -> f != null && f.getStaffBehaviorRating() != null)
            .mapToDouble(f -> f.getStaffBehaviorRating().doubleValue())
            .average()
            .orElse(0.0);
        stats.setAverageStaffBehavior(avgStaffBehavior);
        
        double avgTimeliness = feedbackList.stream()
            .filter(f -> f != null && f.getTimelinessRating() != null)
            .mapToDouble(f -> f.getTimelinessRating().doubleValue())
            .average()
            .orElse(0.0);
        stats.setAverageTimeliness(avgTimeliness);
        
        return stats;
    }

    // Helper classes for feedback statistics
    public static class FeedbackStatistics {
        private int totalFeedback;
        private int responseRequiredCount;
        private int respondedCount;
        private int pendingResponseCount;
        private double averageOverallRating;
        private double averageServiceQuality;
        private double averageStaffBehavior;
        private double averageTimeliness;

        // Getters and setters
        public int getTotalFeedback() { return totalFeedback; }
        public void setTotalFeedback(int totalFeedback) { this.totalFeedback = totalFeedback; }
        
        public int getResponseRequiredCount() { return responseRequiredCount; }
        public void setResponseRequiredCount(int responseRequiredCount) { this.responseRequiredCount = responseRequiredCount; }
        
        public int getRespondedCount() { return respondedCount; }
        public void setRespondedCount(int respondedCount) { this.respondedCount = respondedCount; }
        
        public int getPendingResponseCount() { return pendingResponseCount; }
        public void setPendingResponseCount(int pendingResponseCount) { this.pendingResponseCount = pendingResponseCount; }
        
        public double getAverageOverallRating() { return averageOverallRating; }
        public void setAverageOverallRating(double averageOverallRating) { this.averageOverallRating = averageOverallRating; }
        
        public double getAverageServiceQuality() { return averageServiceQuality; }
        public void setAverageServiceQuality(double averageServiceQuality) { this.averageServiceQuality = averageServiceQuality; }
        
        public double getAverageStaffBehavior() { return averageStaffBehavior; }
        public void setAverageStaffBehavior(double averageStaffBehavior) { this.averageStaffBehavior = averageStaffBehavior; }
        
        public double getAverageTimeliness() { return averageTimeliness; }
        public void setAverageTimeliness(double averageTimeliness) { this.averageTimeliness = averageTimeliness; }
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
