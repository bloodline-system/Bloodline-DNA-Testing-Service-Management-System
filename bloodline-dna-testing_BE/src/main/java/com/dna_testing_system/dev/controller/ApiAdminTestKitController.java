package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.TestKitRequest;
import com.dna_testing_system.dev.dto.response.TestKitResponse;
import com.dna_testing_system.dev.enums.KitType;
import com.dna_testing_system.dev.enums.SampleType;
import com.dna_testing_system.dev.service.TestKitService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;




@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/test-kits")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApiAdminTestKitController {

    TestKitService testKitService;

    // GET ALL + filter + pagination + stats
    @GetMapping
    public ResponseEntity<?> getAllTestKits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "all") String availability,
            @RequestParam(defaultValue = "all") String kitType,
            @RequestParam(defaultValue = "all") String sampleType) {

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

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error loading test kits: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to load test kit data");
        }
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTestKitById(@PathVariable Long id) {
        try {
            TestKitResponse testKit = testKitService.GetTestKitResponseById(id);
            return ResponseEntity.ok(testKit);
        } catch (Exception e) {
            log.error("Error loading test kit ID: " + id, e);
            return ResponseEntity.notFound().build();
        }
    }

    // CREATE
    @PostMapping
    public ResponseEntity<?> createTestKit(@Valid @RequestBody TestKitRequest testKitRequest,
                                           BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            testKitService.CreateTestKit(testKitRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body("Test kit created successfully");
        } catch (Exception e) {
            log.error("Error creating test kit: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create test kit: " + e.getMessage());
        }
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTestKit(@PathVariable Long id,
                                           @Valid @RequestBody TestKitRequest testKitRequest,
                                           BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            testKitService.UpdateTestKit(id, testKitRequest);
            return ResponseEntity.ok("Test kit updated successfully");
        } catch (Exception e) {
            log.error("Error updating test kit ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update test kit: " + e.getMessage());
        }
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTestKit(@PathVariable Long id) {
        try {
            testKitService.DeleteTestKit(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting test kit ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete test kit: " + e.getMessage());
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
