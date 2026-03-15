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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/feedback")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApiAdminFeedbackController {

    CustomerFeedbackService customerFeedbackService;
    UserRepository userRepository;

    // GET ALL - với pagination + filter
    @GetMapping
    public ResponseEntity<?> getAllFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String customerName,
            @RequestParam(defaultValue = "") String serviceName,
            @RequestParam(defaultValue = "all") String responseStatus,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            Page<CustomerFeedbackResponse> feedbackPage = customerFeedbackService.getAllFeedbacks(page, size, search);
            List<CustomerFeedbackResponse> allFeedback = feedbackPage.getContent();

            List<CustomerFeedbackResponse> filteredFeedback = allFeedback.stream()
                    .filter(f -> f != null)
                    .filter(f -> {
                        boolean matchesCustomer = customerName.isEmpty() ||
                                (f.getCustomerName() != null && f.getCustomerName().toLowerCase().contains(customerName.toLowerCase()));
                        boolean matchesService = serviceName.isEmpty() ||
                                (f.getServiceName() != null && f.getServiceName().toLowerCase().contains(serviceName.toLowerCase()));
                        boolean matchesStatus = "all".equals(responseStatus) ||
                                ("responded".equals(responseStatus) && f.getRespondedAt() != null) ||
                                ("unresponded".equals(responseStatus) && f.getRespondedAt() == null);
                        return matchesCustomer && matchesService && matchesStatus;
                    })
                    .collect(Collectors.toList());

            FeedbackStatistics stats = calculateFeedbackStatistics(allFeedback);

            Map<String, Object> response = Map.of(
                    "feedbackList", filteredFeedback,
                    "totalFeedback", feedbackPage.getTotalElements(),
                    "totalPages", feedbackPage.getTotalPages(),
                    "currentPage", page,
                    "pageSize", size,
                    "feedbackStats", stats
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error loading customer feedback: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to load feedback data: " + e.getMessage());
        }
    }

    // GET BY ID
    @GetMapping("/{feedbackId}")
    public ResponseEntity<?> getFeedbackById(@PathVariable Long feedbackId) {
        try {
            CustomerFeedbackResponse feedback = customerFeedbackService.getFeedbackById(feedbackId);
            if (feedback == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            log.error("Error loading feedback details for ID: " + feedbackId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to load feedback details");
        }
    }

    // RESPOND TO FEEDBACK
    @PostMapping("/{feedbackId}")
    public ResponseEntity<?> respondToFeedback(
            @PathVariable Long feedbackId,
            @RequestBody Map<String, String> body) {

        String responseContent = body.get("responseContent");
        if (responseContent == null || responseContent.isBlank()) {
            return ResponseEntity.badRequest().body("responseContent không được để trống");
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            var existingUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));

            RespondFeedbackRequest request = RespondFeedbackRequest.builder()
                    .respondByUserId(existingUser.getId())
                    .responseContent(responseContent)
                    .build();

            customerFeedbackService.respondToFeedback(feedbackId, request);
            return ResponseEntity.ok("Response submitted successfully");

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        } catch (Exception e) {
            log.error("Error responding to feedback: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to submit response: " + e.getMessage());
        }
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

