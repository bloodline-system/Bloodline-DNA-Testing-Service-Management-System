package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.CreateFeedbackRequest;
import com.dna_testing_system.dev.dto.response.CustomerFeedbackResponse;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.exception.EntityNotFoundException;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.CustomerFeedbackService;
import com.dna_testing_system.dev.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserFeedbackController {

    CustomerFeedbackService customerFeedbackService;
    UserProfileService userProfileService;
    UserRepository userRepository;

    @PostMapping("/feedback/create")
    public String createFeedback(@Valid @ModelAttribute CreateFeedbackRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        
        log.info("Received feedback creation request: {}", request);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthenticated user attempting to submit feedback");
            redirectAttributes.addFlashAttribute("error", "You must be logged in to submit feedback.");
            return "redirect:/signin";
        }
        
        try {
            var existingUser = userRepository.findByUsername(authentication.getName());
            String currentUserId = existingUser.orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS)).getId();
            request.setCustomerId(currentUserId);
            
            log.info("Setting customer ID: {} for feedback", currentUserId);

            if (bindingResult.hasErrors()) {
                log.warn("Validation errors in feedback form: {}", bindingResult.getAllErrors());
                redirectAttributes.addFlashAttribute("error", "Please fix the errors in the feedback form.");
                return "redirect:/user/order-details?orderId=" + request.getOrderId();
            }

            CustomerFeedbackResponse feedback = customerFeedbackService.createFeedback(request);
            redirectAttributes.addFlashAttribute("success", "Feedback submitted successfully! Thank you for your review.");
            log.info("Feedback created successfully for order ID: {} by user: {}", request.getOrderId(), currentUserId);
            
        } catch (Exception e) {
            log.error("Error creating feedback for order ID: {} by user: {}", request.getOrderId(), authentication.getName(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to submit feedback: " + e.getMessage());
        }

        return "redirect:/user/order-details?orderId=" + request.getOrderId();
    }

    @GetMapping("/feedback")
    public String viewMyFeedback(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/signin";
        }

        var existingUser = userRepository.findByUsername(authentication.getName());
        String currentUserId = existingUser.orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS)).getId();
        
        try {
            UserProfileResponse userProfile = userProfileService.getUserProfile(currentUserId);
            model.addAttribute("userProfile", userProfile);

            var feedbackList = customerFeedbackService.getFeedbackByCustomer(currentUserId);
            model.addAttribute("feedbackList", feedbackList);

            log.error("loading feedback for user: {}", currentUserId);
            
        } catch (Exception e) {
            log.error("Error loading feedback for user: {}", currentUserId, e);
            model.addAttribute("error", "Unable to load your feedback history.");
        }

        return "redirect:/user/order-details?orderId=\" + request.getOrderId()";
    }
}
