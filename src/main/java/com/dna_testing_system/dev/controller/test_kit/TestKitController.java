package com.dna_testing_system.dev.controller.test_kit;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.test_kit.TestKitRequest;
import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.dto.response.test_kit.TestKitResponse;
import com.dna_testing_system.dev.service.staff.TestKitService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/test-kits")
@SecurityRequirement(name = "bearerAuth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestKitController {

    TestKitService testKitService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<TestKitResponse>> getAllTestKits(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        List<TestKitResponse> allTestKits = testKitService.GetTestKitResponseList();
        Page<TestKitResponse> page = createPage(allTestKits, pageable);
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Test kits retrieved successfully",
                PageResponse.from(page));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<TestKitResponse> getTestKitById(@PathVariable Long id) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Test kit retrieved successfully",
                testKitService.GetTestKitResponseById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createTestKit(@Valid @RequestBody TestKitRequest request) {
        testKitService.CreateTestKit(request);
        return ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Test kit created successfully",
                null);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updateTestKit(
            @PathVariable Long id,
            @Valid @RequestBody TestKitRequest request) {
        testKitService.UpdateTestKit(id, request);
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Test kit updated successfully",
                null);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteTestKit(@PathVariable Long id) {
        testKitService.DeleteTestKit(id);
        return ApiResponse.success(
                HttpStatus.NO_CONTENT.value(),
                "Test kit deleted successfully",
                null);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<TestKitResponse>> searchTestKits(
            @RequestParam String query,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        List<TestKitResponse> searchResults = testKitService.searchTestKits(query);
        Page<TestKitResponse> page = createPage(searchResults, pageable);
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Test kits found successfully",
                PageResponse.from(page));
    }

    private Page<TestKitResponse> createPage(List<TestKitResponse> items, Pageable pageable) {
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, items.size());

        List<TestKitResponse> pageContent = items.stream()
                .skip(start)
                .limit(pageSize)
                .toList();

        return new PageImpl<>(pageContent, pageable, items.size());
    }
}
