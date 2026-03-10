package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.MedicalServiceRequest;
import com.dna_testing_system.dev.dto.request.MedicalServiceUpdateRequest;
import com.dna_testing_system.dev.dto.response.MedicalServiceResponse;
import com.dna_testing_system.dev.service.MedicalServiceManageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * REST Controller for medical service operations
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/manager/services")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServiceController {

    MedicalServiceManageService medicalServiceManageService;

    @GetMapping("/list")
    public ResponseEntity<List<MedicalServiceResponse>> getAllServices() {
        return ResponseEntity.ok(medicalServiceManageService.getAllServices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalServiceResponse> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(medicalServiceManageService.getServiceById(id));
    }

    @PostMapping("/add")
    public String createService(@ModelAttribute MedicalServiceRequest request, RedirectAttributes redirectAttributes) {
        medicalServiceManageService.createService(request);
        return "redirect:/manager/services";
    }

    @PutMapping("/{id}")
    public String updateService(
            @PathVariable Long id,
            @ModelAttribute MedicalServiceUpdateRequest request) {
        medicalServiceManageService.updateService(id, request);
        return "redirect:/manager/services";
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updatesService(@PathVariable Long id, @ModelAttribute MedicalServiceUpdateRequest medicalServiceUpdateRequest) {
        try {
            medicalServiceManageService.updateService(id, medicalServiceUpdateRequest);
        }catch (Exception e){
            return ResponseEntity.badRequest().build();
        }
//        medicalServiceManageService.updateService(id, medicalServiceUpdateRequest);
        return ResponseEntity.ok("ok");
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        medicalServiceManageService.deleteService(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchServices(@RequestParam String query) {
        return ResponseEntity.ok(medicalServiceManageService.searchServiceByNameContaining(query));
    }
}
