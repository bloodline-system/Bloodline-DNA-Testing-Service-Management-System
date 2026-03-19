package com.dna_testing_system.dev.controller.medical_service;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.medical_service.ServiceTypeRequest;
import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.dto.response.medical_service.ServiceTypeResponse;
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
@RequestMapping("/api/v1/manager/service-types")
@SecurityRequirement(name = "bearerAuth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServiceTypeController {

    MedicalServiceManageService medicalServiceManageService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<ServiceTypeResponse>> getAllServiceTypes(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Service types retrieved successfully",
                PageResponse.from(medicalServiceManageService.getServiceTypesPage(pageable)));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ServiceTypeResponse> getServiceTypeById(@PathVariable Long id) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Service type retrieved successfully",
                medicalServiceManageService.getServiceTypeById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ServiceTypeResponse> createServiceType(@Valid @RequestBody ServiceTypeRequest request) {
        return ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Service type created successfully",
                medicalServiceManageService.createServiceType(request));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updateServiceType(
            @PathVariable Long id,
            @Valid @RequestBody ServiceTypeRequest request) {
        medicalServiceManageService.updateTypeService(id, request);
        return ApiResponse.success(HttpStatus.OK.value(), "Service type updated successfully", null);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteServiceType(@PathVariable Long id) {
        medicalServiceManageService.deleteTypeService(id);
        return ApiResponse.success(HttpStatus.NO_CONTENT.value(), "Service type deleted successfully", null);
    }
}
