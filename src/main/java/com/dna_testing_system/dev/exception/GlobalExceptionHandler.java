package com.dna_testing_system.dev.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles authentication exceptions for web requests
     * Returns the user to the login page with an error message
     */
    @ExceptionHandler(AuthenticationException.class)
    public String handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        
        log.error("Authentication error: {} (Error Code: {})",
                ex.getMessage(), ex.getErrorCode());
        
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return getCurrentViewName(request);
    }

    /**
     * Handles service exceptions for web requests
     * Returns the present page with an error message
     */
    @ExceptionHandler(MedicalServiceException.class)
    public String handleMedicalServiceException(
            AuthenticationException ex,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {

        log.error("Authentication error: {} (Error Code: {})",
                ex.getMessage(), ex.getErrorCode());

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return getCurrentViewName(request);
    }

    /**
     * Handles resource not found exceptions for web requests
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        
        log.error("Resource not found: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        
        // Return the current view or a default error page
        return getCurrentViewName(request);
    }

    
    /**
     * Handles all other exceptions for web requests
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(
            Exception ex,
            RedirectAttributes redirectAttributes) {
        
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred. Please try again later.");
        
        return "redirect:/access-denied";
    }
    
    /**
     * Extracts the current view name from the request
     */
    private String getCurrentViewName(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        if (!contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        
        // Default to home page if URI is empty
        if (uri.isEmpty()) {
            uri = "index";
        }
        
        // Remove extensions if present
        if (uri.contains(".")) {
            uri = uri.substring(0, uri.lastIndexOf("."));
        }
        
        return "redirect:/" + uri;
    }
}
