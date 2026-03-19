package com.dna_testing_system.dev.controller.medical_service;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.medical_service.MedicalServiceRequest;
import com.dna_testing_system.dev.dto.request.medical_service.MedicalServiceUpdateRequest;
import com.dna_testing_system.dev.dto.response.medical_service.MedicalServiceFilterResponse;
import com.dna_testing_system.dev.dto.response.medical_service.MedicalServiceResponse;
import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.service.service.MedicalServiceManageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager/services")
@SecurityRequirement(name = "bearerAuth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServiceController {

    MedicalServiceManageService medicalServiceManageService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<MedicalServiceResponse>> getAllServices(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Services retrieved successfully",
                PageResponse.from(medicalServiceManageService.getServicesPage(pageable)));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<MedicalServiceResponse> getServiceById(@PathVariable Long id) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Service retrieved successfully",
                medicalServiceManageService.getServiceById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MedicalServiceResponse> createService(@Valid @RequestBody MedicalServiceRequest request) {
        return ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Service created successfully",
                medicalServiceManageService.createService(request));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updateService(
            @PathVariable Long id,
            @Valid @RequestBody MedicalServiceUpdateRequest request) {
        medicalServiceManageService.updateService(id, request);
        return ApiResponse.success(HttpStatus.OK.value(), "Service updated successfully", null);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteService(@PathVariable Long id) {
        medicalServiceManageService.deleteService(id);
        return ApiResponse.success(HttpStatus.NO_CONTENT.value(), "Service deleted successfully", null);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<MedicalServiceFilterResponse>> searchServices(
            @RequestParam String query,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Services found successfully",
                PageResponse.from(medicalServiceManageService.searchServicesPage(query, pageable)));
    }
}
