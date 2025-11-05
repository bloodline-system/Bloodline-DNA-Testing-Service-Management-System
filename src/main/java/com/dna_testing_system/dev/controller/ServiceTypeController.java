package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.ServiceTypeRequest;
import com.dna_testing_system.dev.service.MedicalServiceManageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for service type management
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/manager/service-types")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServiceTypeController {

    MedicalServiceManageService medicalServiceManageService;

    /**
     * Add new service type (POST request from modal form)
     */
    @PostMapping("/add")
    public String addServiceType(
            @RequestParam("typeName") String typeName,
            @RequestParam(value = "isActive", defaultValue = "true") Boolean isActive,
            RedirectAttributes redirectAttributes) {

        try {
            // Create ServiceTypeRequest using your existing DTO
            ServiceTypeRequest request = ServiceTypeRequest.builder()
                    .typeName(typeName)
                    .isActive(isActive)
                    .build();

            // Use your existing service method
            medicalServiceManageService.createServiceType(request);

            redirectAttributes.addFlashAttribute("successMessage", "Service type '" + typeName + "' added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding service type: " + e.getMessage());
        }

        return "redirect:/manager/services";
    }

    /**
     * Get service type details (for editing if needed in future)
     */
    @GetMapping("/{id}")
    @ResponseBody
    public String getServiceType(@PathVariable Long id) {
        try {
            // You can implement getServiceTypeById in your service if needed
            return "{\"success\":true,\"message\":\"Service type found\"}";
        } catch (Exception e) {
            return "{\"error\":\"Service type not found\"}";
        }
    }

    // ...existing code...

/**
 * Update service type
 */
@PutMapping("/update/{id}")
@ResponseBody
public ResponseEntity<String> updateServiceType(
        @PathVariable Long id,
        @RequestParam("typeName") String typeName,
        @RequestParam("isActive") Boolean isActive) {
    
    try {
        ServiceTypeRequest request = ServiceTypeRequest.builder()
                .typeName(typeName)
                .isActive(isActive)
                .build();
        
        // You'll need to implement updateServiceType in your service
        medicalServiceManageService.updateTypeService(id, request);

        return ResponseEntity.ok("{\"success\":true,\"message\":\"Service type updated successfully\"}");
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("{\"success\":false,\"message\":\"Error updating service type: " + e.getMessage() + "\"}");
    }
}

/**
 * Delete service type
 */
@DeleteMapping("/delete/{id}")
@ResponseBody
public ResponseEntity<String> deleteServiceType(@PathVariable Long id) {
    try {
        // You'll need to implement deleteServiceType in your service
        medicalServiceManageService.deleteTypeService(id);
        return ResponseEntity.ok("{\"success\":true,\"message\":\"Service type deleted successfully\"}");
    } catch (Exception e) {
        return ResponseEntity.badRequest().build();
    }
}

// ...existing code...
}