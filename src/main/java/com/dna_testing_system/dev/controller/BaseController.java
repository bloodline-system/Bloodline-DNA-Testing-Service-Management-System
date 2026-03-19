package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Slf4j
@Controller
public abstract class BaseController {

    @Autowired
    protected UserProfileService userProfileService;

    protected void addUserProfileToModel(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {

            String currentPrincipalName = authentication.getName();
            UserProfileResponse userProfile = userProfileService.getUserProfile(currentPrincipalName);
            model.addAttribute("userProfile", userProfile);
        }
    }
}