package com.dna_testing_system.dev.controller.staff;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.staff.TestResultsResquest;
import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.dto.response.staff.TestResultsResponse;
import com.dna_testing_system.dev.service.FileEdit;
import com.dna_testing_system.dev.service.staff.StaffService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff/test-results")
@SecurityRequirement(name = "bearerAuth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StaffTestResultController {

    StaffService staffService;
    FileEdit fileEdit;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<TestResultsResponse>> getTestResults(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        String username = currentUsername();
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Test results retrieved successfully",
                PageResponse.from(staffService.getTestResultsPage(username, status, pageable)));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<TestResultsResponse> getTestResultById(@PathVariable Long id) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Test result retrieved successfully",
                staffService.getTestResultById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updateTestResult(
            @PathVariable Long id,
            @RequestParam(value = "reportFilePath", required = false) MultipartFile reportFile,
            @ModelAttribute TestResultsResquest request) {
        TestResultsResponse existing = staffService.getTestResultById(id);

        if (reportFile == null || reportFile.isEmpty()) {
            request.setReportFile(existing.getReportFile());
        } else if (existing.getReportFile() == null) {
            request.setReportFile(fileEdit.editFile(reportFile));
        } else {
            fileEdit.deleteFile(existing.getReportFile());
            request.setReportFile(fileEdit.editFile(reportFile));
        }

        if (request.getTestDate() == null) {
            request.setTestDate(existing.getTestDate());
        }
        request.setReportGenerated(true);

        staffService.updateTestResult(request, id);
        return ApiResponse.success(HttpStatus.OK.value(), "Test result updated successfully", null);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
