package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.response.MedicalServiceResponse;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.service.MedicalServiceManageService;
import com.dna_testing_system.dev.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HomeController {

    UserProfileService userProfileService;
    MedicalServiceManageService medicalServiceManageService;

    @GetMapping("/")
    public String index(Model model) {
        // Add pricing data to the index page
        addPricingDataToModel(model);
        return "index";
    }

    @GetMapping("/user/home")
    public String userHomePage(Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {

            String currentPrincipalName = authentication.getName();
            UserProfileResponse userProfile = userProfileService.getUserProfile(currentPrincipalName);
            model.addAttribute("userProfile", userProfile);
        }

        // Add pricing data to the user home page
        addPricingDataToModel(model);
        return "user/home";
    }

    private void addPricingDataToModel(Model model) {
        try {
            // Get all available services
            List<MedicalServiceResponse> allServices = medicalServiceManageService.getAllServices();
            
            // Filter only available services
            List<MedicalServiceResponse> availableServices = allServices.stream()
                    .filter(service -> service.getIsAvailable() != null && service.getIsAvailable())
                    .collect(Collectors.toList());
            
            // Split services for individual consumers (first 3)
            List<MedicalServiceResponse> individualServices = availableServices.stream()
                    .limit(3)
                    .collect(Collectors.toList());
            
            // Split services for clinical providers (next 3, or same if not enough)
            List<MedicalServiceResponse> clinicalServices = availableServices.stream()
                    .skip(individualServices.size())
                    .limit(3)
                    .collect(Collectors.toList());
            
            // If we don't have enough services for clinical, use the same ones
            if (clinicalServices.isEmpty() && !individualServices.isEmpty()) {
                clinicalServices = individualServices;
            }
            
            model.addAttribute("individualServices", individualServices);
            model.addAttribute("clinicalServices", clinicalServices);
            
        } catch (Exception e) {
            // Handle any service errors gracefully
            model.addAttribute("individualServices", List.of());
            model.addAttribute("clinicalServices", List.of());
        }
    }

}