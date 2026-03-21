package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.response.MedicalServiceResponse;
import com.dna_testing_system.dev.dto.response.PublicPricingResponse;
import com.dna_testing_system.dev.service.MedicalServiceManageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublicController {

    MedicalServiceManageService medicalServiceManageService;

    @GetMapping("/public/pricing")
    public String pricingPage(Model model) {
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
        
        return "public/pricing";
    }

            @GetMapping("/api/v1/public/pricing")
            @ResponseBody
            public ResponseEntity<ApiResponse<PublicPricingResponse>> pricingApi(HttpServletRequest request) {
                try {
                    List<MedicalServiceResponse> allServices = medicalServiceManageService.getAllServices();

                    List<MedicalServiceResponse> availableServices = allServices.stream()
                            .filter(service -> service.getIsAvailable() != null && service.getIsAvailable())
                            .collect(Collectors.toList());

                    List<MedicalServiceResponse> individualServices = availableServices.stream()
                            .limit(3)
                            .collect(Collectors.toList());

                    List<MedicalServiceResponse> clinicalServices = availableServices.stream()
                            .skip(individualServices.size())
                            .limit(3)
                            .collect(Collectors.toList());

                    if (clinicalServices.isEmpty() && !individualServices.isEmpty()) {
                        clinicalServices = individualServices;
                    }

                    PublicPricingResponse response = new PublicPricingResponse(individualServices, clinicalServices);
                    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Get pricing successfully", response));
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to load pricing", request.getRequestURI()));
                }
            }
}
