package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.response.NotificationActivityResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {
    NotificationService notificationService;
    UserRepository userRepository;
    @ModelAttribute("notifications")
    public List<NotificationActivityResponse> getNotifications() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null &&
                    authentication.isAuthenticated() &&
                    !"anonymousUser".equals(authentication.getPrincipal())) {

                User user = userRepository.findUsersByUsername(authentication.getName());
                return notificationService.getAllNotifications(user);
            }

            return Collections.emptyList();
        } catch (Exception e) {
            // Log error và return empty list để tránh page bị break
            System.err.println("Error loading notifications: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}