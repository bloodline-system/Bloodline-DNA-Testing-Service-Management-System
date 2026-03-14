package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.CreateFeedbackRequest;
import com.dna_testing_system.dev.dto.response.CustomerFeedbackResponse;
import com.dna_testing_system.dev.exception.EntityNotFoundException;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.CustomerFeedbackService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/feedback")
@SecurityRequirement(name = "bearerAuth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserFeedbackController {

    CustomerFeedbackService customerFeedbackService;
    UserRepository userRepository;

    @PostMapping("/{feedbackId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<CustomerFeedbackResponse>> createFeedback(@PathVariable String feedbackId,
                                                                                @Valid @ModelAttribute CreateFeedbackRequest request,
                                                                                BindingResult bindingResult,
                                                                                HttpServletRequest httpServletRequest) {

        log.info("Received feedback creation request for feedbackId={}: {}", feedbackId, request);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthenticated user attempting to submit feedback");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "You must be logged in to submit feedback.", httpServletRequest.getRequestURI()));
        }

        try {
            var existingUser = userRepository.findByUsername(authentication.getName());
            String currentUserId = existingUser.orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS)).getId();
            request.setCustomerId(currentUserId);

            log.info("Setting customer ID: {} for feedback", currentUserId);

            if (bindingResult.hasErrors()) {
                log.warn("Validation errors in feedback form: {}", bindingResult.getAllErrors());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Please fix the errors in the feedback form.", httpServletRequest.getRequestURI()));
            }

            CustomerFeedbackResponse feedback = customerFeedbackService.createFeedback(request);
            log.info("Feedback created successfully for order ID: {} by user: {}", request.getOrderId(), currentUserId);

            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Feedback submitted successfully!", feedback)
            );
        } catch (Exception e) {
            log.error("Error creating feedback for order ID: {} by user: {}", request.getOrderId(), authentication.getName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to submit feedback: " + e.getMessage(), httpServletRequest.getRequestURI()));
        }
    }

    @GetMapping
    @ResponseBody
    public ResponseEntity<ApiResponse<List<CustomerFeedbackResponse>>> viewMyFeedback(HttpServletRequest httpServletRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "You must be logged in.", httpServletRequest.getRequestURI()));
        }

        var existingUser = userRepository.findByUsername(authentication.getName());
        String currentUserId = existingUser.orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS)).getId();

        try {
            var feedbackList = customerFeedbackService.getFeedbackByCustomer(currentUserId);
            log.info("Loaded feedback for user: {}", currentUserId);

            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Get feedback list successfully", feedbackList)
            );
        } catch (Exception e) {
            log.error("Error loading feedback for user: {}", currentUserId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to load your feedback history.", httpServletRequest.getRequestURI()));
        }
    }

    @GetMapping("/{feedbackId}")
    @ResponseBody
    public ResponseEntity<ApiResponse<CustomerFeedbackResponse>> getFeedbackById(@PathVariable Long feedbackId,
                                                                                  HttpServletRequest httpServletRequest) {
        try {
            CustomerFeedbackResponse feedback = customerFeedbackService.getFeedbackById(feedbackId);
            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Get feedback successfully", feedback)
            );
        } catch (Exception e) {
            log.error("Error loading feedback by id: {}", feedbackId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Feedback not found", httpServletRequest.getRequestURI()));
        }
    }
}
