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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/manager")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManagerTestKitController extends BaseController {

    TestKitService testKitService;

    @GetMapping("/test-kit-management")
    public String testKitManagement(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(defaultValue = "") String search,
                                   @RequestParam(defaultValue = "all") String availability,
                                   @RequestParam(defaultValue = "all") String kitType,
                                   @RequestParam(defaultValue = "all") String sampleType,
                                   Model model) {
        addUserProfileToModel(model);
        
        try {
            // Get all test kits
            List<TestKitResponse> allTestKits = testKitService.GetTestKitResponseList();
            
            // Apply filters
            List<TestKitResponse> filteredTestKits = allTestKits.stream()
                .filter(kit -> {
                    // Search filter
                    boolean matchesSearch = search.isEmpty() || 
                        (kit.getKitName() != null && kit.getKitName().toLowerCase().contains(search.toLowerCase()));
                    
                    // Availability filter
                    boolean matchesAvailability = "all".equals(availability) ||
                        ("available".equals(availability) && Boolean.TRUE.equals(kit.getIsAvailable())) ||
                        ("unavailable".equals(availability) && Boolean.FALSE.equals(kit.getIsAvailable()));
                    
                    // Kit type filter
                    boolean matchesKitType = "all".equals(kitType) ||
                        (kit.getKitType() != null && kit.getKitType().equalsIgnoreCase(kitType));
                    
                    // Sample type filter
                    boolean matchesSampleType = "all".equals(sampleType) ||
                        (kit.getSampleType() != null && kit.getSampleType().equalsIgnoreCase(sampleType));
                    
                    return matchesSearch && matchesAvailability && matchesKitType && matchesSampleType;
                })
                .collect(Collectors.toList());
            
            // Manual pagination
            int start = Math.min(page * size, filteredTestKits.size());
            int end = Math.min(start + size, filteredTestKits.size());
            List<TestKitResponse> pageContent = filteredTestKits.subList(start, end);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<TestKitResponse> testKitPage = new PageImpl<>(pageContent, pageable, filteredTestKits.size());
            
            // Calculate statistics
            TestKitStatistics stats = calculateTestKitStatistics(allTestKits);
            
            // Add attributes to model
            model.addAttribute("testKits", testKitPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", testKitPage.getTotalPages());
            model.addAttribute("totalElements", testKitPage.getTotalElements());
            model.addAttribute("size", size);
            model.addAttribute("search", search);
            model.addAttribute("selectedAvailability", availability);
            model.addAttribute("selectedKitType", kitType);
            model.addAttribute("selectedSampleType", sampleType);
            model.addAttribute("stats", stats);
            
            // Add enum values for dropdowns
            model.addAttribute("kitTypes", KitType.values());
            model.addAttribute("sampleTypes", SampleType.values());
            
            // Add empty form object for create modal
            model.addAttribute("testKitRequest", new TestKitRequest());
            
        } catch (Exception e) {
            log.error("Error loading test kit management page: ", e);
            model.addAttribute("error", "Unable to load test kit data");
            model.addAttribute("testKits", new ArrayList<>());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0);
        }

        return "manager/test-kit-management";
    }

    @GetMapping("/test-kit-management/{id}")
    public String testKitDetails(@PathVariable Long id, Model model) {
        addUserProfileToModel(model);
        
        try {
            TestKitResponse testKit = testKitService.GetTestKitResponseById(id);
            model.addAttribute("testKit", testKit);
        } catch (Exception e) {
            log.error("Error loading test kit details for ID: " + id, e);
            model.addAttribute("error", "Test kit not found");
            return "redirect:/manager/test-kit-management";
        }
        
        return "manager/test-kit-details";
    }

    @PostMapping("/test-kit-management/create")
    public String createTestKit(@Valid @ModelAttribute TestKitRequest testKitRequest,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct the form errors");
            return "redirect:/manager/test-kit-management";
        }
        
        try {
            testKitService.CreateTestKit(testKitRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Test kit created successfully");
        } catch (Exception e) {
            log.error("Error creating test kit: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create test kit: " + e.getMessage());
        }
        
        return "redirect:/manager/test-kit-management";
    }

    @GetMapping("/test-kit-management/{id}/edit")
    public String editTestKitForm(@PathVariable Long id, Model model) {
        addUserProfileToModel(model);
        
        try {
            TestKitResponse testKit = testKitService.GetTestKitResponseById(id);
            
            // Convert TestKitResponse to TestKitRequest for editing
            TestKitRequest testKitRequest = TestKitRequest.builder()
                .kitName(testKit.getKitName())
                .kitType(KitType.valueOf(testKit.getKitType()))
                .sampleType(SampleType.valueOf(testKit.getSampleType()))
                .basePrice(testKit.getBasePrice())
                .currentPrice(testKit.getCurrentPrice())
                .quantityInStock(testKit.getQuantityInStock())
                .kitDescription(testKit.getKitDescription())
                .expiryDate(testKit.getExpiryDate())
                .producedBy(testKit.getProducedBy())
                .isAvailable(testKit.getIsAvailable())
                .build();
            
            model.addAttribute("testKitRequest", testKitRequest);
            model.addAttribute("testKitId", id);
            model.addAttribute("kitTypes", KitType.values());
            model.addAttribute("sampleTypes", SampleType.values());
            
        } catch (Exception e) {
            log.error("Error loading test kit for editing ID: " + id, e);
            model.addAttribute("error", "Test kit not found");
            return "redirect:/manager/test-kit-management";
        }
        
        return "manager/test-kit-edit";
    }

    @PostMapping("/test-kit-management/{id}/update")
    public String updateTestKit(@PathVariable Long id,
                               @Valid @ModelAttribute TestKitRequest testKitRequest,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct the form errors");
            return "redirect:/manager/test-kit-management/" + id + "/edit";
        }
        
        try {
            testKitService.UpdateTestKit(id, testKitRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Test kit updated successfully");
        } catch (Exception e) {
            log.error("Error updating test kit ID: " + id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update test kit: " + e.getMessage());
        }
        
        return "redirect:/manager/test-kit-management";
    }

    @PostMapping("/test-kit-management/{id}/delete")
    public String deleteTestKit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        
        try {
            testKitService.DeleteTestKit(id);
            redirectAttributes.addFlashAttribute("successMessage", "Test kit deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting test kit ID: " + id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete test kit: " + e.getMessage());
        }
        
        return "redirect:/manager/test-kit-management";
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
