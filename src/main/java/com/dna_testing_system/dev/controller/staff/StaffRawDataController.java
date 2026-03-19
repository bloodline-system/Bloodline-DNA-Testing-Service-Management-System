package com.dna_testing_system.dev.controller.staff;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.staff.RawDataRequest;
import com.dna_testing_system.dev.dto.response.RawDataResponse;
import com.dna_testing_system.dev.service.FileEdit;
import com.dna_testing_system.dev.service.staff.StaffService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff")
@SecurityRequirement(name = "bearerAuth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StaffRawDataController {

    StaffService staffService;
    FileEdit fileEdit;

    @GetMapping("/raw-data/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<RawDataResponse> getRawDataById(@PathVariable Long id) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Raw data retrieved successfully",
                staffService.getRawDataById(id));
    }

    @PostMapping(value = "/test-results/{testResultId}/raw-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createRawData(
            @PathVariable Long testResultId,
            @RequestParam(value = "filePath", required = false) MultipartFile file,
            @ModelAttribute RawDataRequest request) {
        if (file != null && !file.isEmpty()) {
            request.setFile(fileEdit.editFile(file));
        }
        if (request.getCollectionDate() == null) {
            request.setCollectionDate(LocalDateTime.now());
        }
        staffService.createRawData(request, testResultId);
        return ApiResponse.success(HttpStatus.CREATED.value(), "Raw data created successfully", null);
    }

    @PutMapping(value = "/raw-data/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updateRawData(
            @PathVariable Long id,
            @RequestParam(value = "filePath", required = false) MultipartFile file,
            @ModelAttribute RawDataRequest request) {
        RawDataResponse existing = staffService.getRawDataById(id);

        if (file == null || file.isEmpty()) {
            request.setFile(existing.getFilePath());
        } else if (existing.getFilePath() == null) {
            request.setFile(fileEdit.editFile(file));
        } else {
            fileEdit.deleteFile(existing.getFilePath());
            request.setFile(fileEdit.editFile(file));
        }

        if (request.getCollectionDate() == null) {
            request.setCollectionDate(existing.getCollectedAt());
        }

        staffService.updateRawData(request, id);
        return ApiResponse.success(HttpStatus.OK.value(), "Raw data updated successfully", null);
    }
}
