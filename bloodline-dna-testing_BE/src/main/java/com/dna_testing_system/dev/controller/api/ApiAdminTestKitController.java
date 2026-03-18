package com.dna_testing_system.dev.controller.api;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.TestKitRequest;
import com.dna_testing_system.dev.dto.response.TestKitResponse;
import com.dna_testing_system.dev.enums.KitType;
import com.dna_testing_system.dev.enums.SampleType;
import com.dna_testing_system.dev.service.TestKitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;




@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/test-kits")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApiAdminTestKitController {

    TestKitService testKitService;

    // GET ALL + filter + pagination + stats
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllTestKits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "all") String availability,
            @RequestParam(defaultValue = "all") String kitType,
            @RequestParam(defaultValue = "all") String sampleType,
            HttpServletRequest request) {

        try {
            List<TestKitResponse> allTestKits = testKitService.GetTestKitResponseList();

            List<TestKitResponse> filtered = allTestKits.stream()
                    .filter(kit -> {
                        boolean matchesSearch = search.isEmpty() ||
                                (kit.getKitName() != null && kit.getKitName().toLowerCase().contains(search.toLowerCase()));
                        boolean matchesAvailability = "all".equals(availability) ||
                                ("available".equals(availability) && Boolean.TRUE.equals(kit.getIsAvailable())) ||
                                ("unavailable".equals(availability) && Boolean.FALSE.equals(kit.getIsAvailable()));
                        boolean matchesKitType = "all".equals(kitType) ||
                                (kit.getKitType() != null && kit.getKitType().equalsIgnoreCase(kitType));
                        boolean matchesSampleType = "all".equals(sampleType) ||
                                (kit.getSampleType() != null && kit.getSampleType().equalsIgnoreCase(sampleType));
                        return matchesSearch && matchesAvailability && matchesKitType && matchesSampleType;
                    })
                    .collect(Collectors.toList());

            // Manual pagination
            int start = Math.min(page * size, filtered.size());
            int end = Math.min(start + size, filtered.size());
            List<TestKitResponse> pageContent = filtered.subList(start, end);

            Page<TestKitResponse> testKitPage = new PageImpl<>(pageContent, PageRequest.of(page, size), filtered.size());
            TestKitStatistics stats = calculateTestKitStatistics(allTestKits);

            Map<String, Object> response = Map.of(
                    "testKits", testKitPage.getContent(),
                    "currentPage", page,
                    "totalPages", testKitPage.getTotalPages(),
                    "totalElements", testKitPage.getTotalElements(),
                    "pageSize", size,
                    "stats", stats,
                    "kitTypes", KitType.values(),
                    "sampleTypes", SampleType.values()
            );

            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Test kits loaded", response));

        } catch (Exception e) {
            log.error("Error loading test kits: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to load test kit data", request.getRequestURI()));
        }
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestKitResponse>> getTestKitById(@PathVariable Long id,
                                                                       HttpServletRequest request) {
        try {
            TestKitResponse testKit = testKitService.GetTestKitResponseById(id);
            if (testKit == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Test kit not found", request.getRequestURI()));
            }
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Test kit loaded", testKit));
        } catch (Exception e) {
            log.error("Error loading test kit ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to load test kit", request.getRequestURI()));
        }
    }

    // CREATE
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createTestKit(@Valid @RequestBody TestKitRequest testKitRequest,
                                                           BindingResult bindingResult,
                                                           HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), String.join(", ", errors), request.getRequestURI()));
        }

        try {
            testKitService.CreateTestKit(testKitRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(HttpStatus.CREATED.value(), "Test kit created successfully", null));
        } catch (Exception e) {
            log.error("Error creating test kit: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to create test kit: " + e.getMessage(), request.getRequestURI()));
        }
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TestKitResponse>> updateTestKit(@PathVariable Long id,
                                                                      @Valid @RequestBody TestKitRequest testKitRequest,
                                                                      BindingResult bindingResult,
                                                                      HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), String.join(", ", errors), request.getRequestURI()));
        }

        try {
            testKitService.UpdateTestKit(id, testKitRequest);
            TestKitResponse updated = testKitService.GetTestKitResponseById(id);
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Test kit updated successfully", updated));
        } catch (Exception e) {
            log.error("Error updating test kit ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to update test kit: " + e.getMessage(), request.getRequestURI()));
        }
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> deleteTestKit(@PathVariable Long id,
                                                              HttpServletRequest request) {
        try {
            testKitService.DeleteTestKit(id);
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Test kit deleted successfully", true));
        } catch (Exception e) {
            log.error("Error deleting test kit ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to delete test kit: " + e.getMessage(), request.getRequestURI()));
        }
    }

    private TestKitStatistics calculateTestKitStatistics(List<TestKitResponse> testKits) {
        TestKitStatistics stats = new TestKitStatistics();

        stats.setTotalKits(testKits.size());
        stats.setAvailableKits((int) testKits.stream().filter(k -> Boolean.TRUE.equals(k.getIsAvailable())).count());
        stats.setUnavailableKits((int) testKits.stream().filter(k -> Boolean.FALSE.equals(k.getIsAvailable())).count());
        stats.setExpiredKits((int) testKits.stream().filter(TestKitResponse::isExpired).count());
        stats.setLowStockKits((int) testKits.stream().filter(TestKitResponse::isLowStock).count());
        stats.setTotalStock(testKits.stream().mapToInt(k -> k.getQuantityInStock() != null ? k.getQuantityInStock() : 0).sum());

        return stats;
    }

    // Helper class for test kit statistics
    public static class TestKitStatistics {
        private int totalKits;
        private int availableKits;
        private int unavailableKits;
        private int expiredKits;
        private int lowStockKits;
        private int totalStock;

        // Getters and setters
        public int getTotalKits() { return totalKits; }
        public void setTotalKits(int totalKits) { this.totalKits = totalKits; }

        public int getAvailableKits() { return availableKits; }
        public void setAvailableKits(int availableKits) { this.availableKits = availableKits; }

        public int getUnavailableKits() { return unavailableKits; }
        public void setUnavailableKits(int unavailableKits) { this.unavailableKits = unavailableKits; }

        public int getExpiredKits() { return expiredKits; }
        public void setExpiredKits(int expiredKits) { this.expiredKits = expiredKits; }

        public int getLowStockKits() { return lowStockKits; }
        public void setLowStockKits(int lowStockKits) { this.lowStockKits = lowStockKits; }

        public int getTotalStock() { return totalStock; }
        public void setTotalStock(int totalStock) { this.totalStock = totalStock; }
    }
}
