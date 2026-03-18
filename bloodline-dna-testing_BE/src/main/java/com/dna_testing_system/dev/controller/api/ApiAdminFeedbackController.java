package com.dna_testing_system.dev.controller.api;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.RespondFeedbackRequest;
import com.dna_testing_system.dev.dto.response.CustomerFeedbackResponse;
import com.dna_testing_system.dev.exception.EntityNotFoundException;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.CustomerFeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/feedback")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApiAdminFeedbackController {

    CustomerFeedbackService customerFeedbackService;
    UserRepository userRepository;

    // GET ALL - with pagination + filter
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String customerName,
            @RequestParam(defaultValue = "") String serviceName,
            @RequestParam(defaultValue = "all") String responseStatus,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {

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

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Feedback list loaded", response));

        } catch (Exception e) {
            log.error("Error loading customer feedback: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to load feedback data: " + e.getMessage(), request.getRequestURI()));
        }
    }

    // GET BY ID
    @GetMapping("/{feedbackId}")
    public ResponseEntity<ApiResponse<CustomerFeedbackResponse>> getFeedbackById(@PathVariable Long feedbackId,
                                                                                 HttpServletRequest request) {
        try {
            CustomerFeedbackResponse feedback = customerFeedbackService.getFeedbackById(feedbackId);
            if (feedback == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Feedback not found", request.getRequestURI()));
            }
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Feedback loaded", feedback));
        } catch (Exception e) {
            log.error("Error loading feedback details for ID: " + feedbackId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to load feedback details", request.getRequestURI()));
        }
    }

    // RESPOND TO FEEDBACK
    @PatchMapping("/{feedbackId}/respond")
    public ResponseEntity<ApiResponse<CustomerFeedbackResponse>> respondToFeedback(
            @PathVariable Long feedbackId,
            @RequestBody RespondFeedbackRequest respondRequest,
            HttpServletRequest request) {

        if (respondRequest == null || respondRequest.getResponseContent() == null || respondRequest.getResponseContent().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "responseContent không được để trống", request.getRequestURI()));
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            var existingUser = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));

            respondRequest.setRespondByUserId(existingUser.getId());

            CustomerFeedbackResponse response = customerFeedbackService.respondToFeedback(feedbackId, respondRequest);
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Response submitted successfully", response));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not found", request.getRequestURI()));
        } catch (Exception e) {
            log.error("Error responding to feedback: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to submit response: " + e.getMessage(), request.getRequestURI()));
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

}

