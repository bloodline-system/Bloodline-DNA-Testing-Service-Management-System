package com.dna_testing_system.dev.controller.staff;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.staff.SampleCollectionStatusUpdateRequest;
import com.dna_testing_system.dev.dto.response.staff.CRUDsampleCollectionResponse;
import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.service.staff.StaffService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff/sample-collections")
@SecurityRequirement(name = "bearerAuth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StaffSampleCollectionController {

    StaffService staffService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<CRUDsampleCollectionResponse>> getSampleCollections(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "collectionId") Pageable pageable) {
        String username = currentUsername();
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Sample collections retrieved successfully",
                PageResponse.from(staffService.getSampleCollectionsPage(username, status, pageable)));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CRUDsampleCollectionResponse> getSampleCollectionById(@PathVariable Long id) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Sample collection retrieved successfully",
                staffService.getSampleCollectionTasksById(id));
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updateSampleCollectionStatus(
            @PathVariable Long id,
            @Valid @RequestBody SampleCollectionStatusUpdateRequest request) {
        staffService.updateSampleCollectionStatus(id, request.collectionStatus(), request.sampleQuality());
        return ApiResponse.success(HttpStatus.OK.value(), "Sample collection status updated successfully", null);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
