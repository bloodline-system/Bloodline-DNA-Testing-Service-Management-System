package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.RawDataRequest;
import com.dna_testing_system.dev.dto.request.TestResultsResquest;
import com.dna_testing_system.dev.dto.request.PaymentUpdatingRequest;
import com.dna_testing_system.dev.dto.response.*;
import com.dna_testing_system.dev.entity.RawTestData;
import com.dna_testing_system.dev.entity.TestResult;
import com.dna_testing_system.dev.enums.PaymentStatus;
import com.dna_testing_system.dev.enums.ServiceOrderStatus;
import com.dna_testing_system.dev.service.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/staff")
public class CRUDstaffController {
    OrderService orderService;
    OrderKitService orderKitService;
    OrderParticipantService orderParticipantService;
    StaffService staffService;
    OrderTaskManagementService orderTaskManagementService;
    FileEdit fileEdit;
    PaymentService paymentService;
    @GetMapping("/list-orders")
    public String listOrders(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        List<CRUDorderResponse> serviceOrders = staffService.getForStaff(currentPrincipalName);
        model.addAttribute("serviceOrders", serviceOrders);
        model.addAttribute("section", "orders");
        return "staff/list-orders"; // Assuming you have a Thymeleaf template named "list-orders.html"
    }

    @GetMapping("/details-order")
    public String detailsOrder(@RequestParam("orderId") Long orderId ,Model model) {
        CRUDorderResponse serviceOrder = staffService.getOrderById(orderId);
        List<OrderTestKitResponse> orderTestKits = orderKitService.getOrderById(orderId);
        List<OrderParticipantResponse> orderParticipants = orderParticipantService.getAllParticipantsByOrderId(orderId);
        model.addAttribute("orderTestKits", orderTestKits);
        model.addAttribute("orderParticipants", orderParticipants);
        model.addAttribute("serviceOrderByCustomerResponse", serviceOrder);

        BigDecimal paymentTotal = BigDecimal.ZERO;
        for( OrderTestKitResponse kit : orderTestKits) {
            paymentTotal = paymentTotal.add(kit.getTotalPrice());
        }
        paymentTotal = paymentTotal.add(serviceOrder.getFinalAmount());
        model.addAttribute("paymentTotal",paymentTotal);

        model.addAttribute("section", "orders");

        return "staff/details-order"; // Assuming you have a Thymeleaf template named "details-order.html"
    }

    @GetMapping("/update-order")
    public String viewUpdateOrder(@RequestParam("orderId") Long orderId ,Model model) {
        CRUDorderResponse serviceOrder = staffService.getOrderById(orderId);
        List<OrderTestKitResponse> orderTestKits = orderKitService.getOrderById(orderId);
        List<OrderParticipantResponse> orderParticipants = orderParticipantService.getAllParticipantsByOrderId(orderId);
        model.addAttribute("orderTestKits", orderTestKits);
        model.addAttribute("orderParticipants", orderParticipants);
        model.addAttribute("serviceOrderByCustomerResponse", serviceOrder);

        BigDecimal paymentTotal = BigDecimal.ZERO;
        for( OrderTestKitResponse kit : orderTestKits) {
            paymentTotal = paymentTotal.add(kit.getTotalPrice());
        }
        paymentTotal = paymentTotal.add(serviceOrder.getFinalAmount());
        model.addAttribute("paymentTotal",paymentTotal);


        model.addAttribute("section", "orders");
        return "staff/update-order";

    }
    @PostMapping("/update-order")
    public String updateOrder(@RequestParam("orderId") Long orderId, @RequestParam("status") String status) {
        orderTaskManagementService.updateOrderStatus(orderId,status);
        return "redirect:/staff/list-orders"; // Redirect to the list of orders after updating
    }
    // create raw data
    @GetMapping("/create-raw-data")
    public String viewCreateRawData(@RequestParam("testResultsId") Long testResultsId, Model model) {
        RawDataRequest rawDataRequest = new RawDataRequest();
        rawDataRequest.setCollectionDate(LocalDateTime.now());
        model.addAttribute("rawData", rawDataRequest);
        model.addAttribute("today", LocalDateTime.now());
        model.addAttribute("testResultId", testResultsId);
        model.addAttribute("section", "test-results");
        return "staff/create-raw-data";
    }

    @PostMapping("/create-raw-data")
    public String createRawData(@RequestParam(value = "filePath", required = false) MultipartFile file,
                              @ModelAttribute RawDataRequest rawDataRequest,
                              @RequestParam("testResultId") Long testResultId,
                              RedirectAttributes redirectAttributes) {
        try {
            if (file != null && !file.isEmpty()) {
                rawDataRequest.setFile(fileEdit.editFile(file));
            }

            // Ensure collection date is set if not provided
            if (rawDataRequest.getCollectionDate() == null) {
                rawDataRequest.setCollectionDate(LocalDateTime.now());
            }

            staffService.createRawData(rawDataRequest, testResultId);
            redirectAttributes.addFlashAttribute("successMessage", "Raw data created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create raw data: " + e.getMessage());
        }
        return "redirect:/staff/list-test-results";
    }
    // end update order status
    @GetMapping("/list-sample-collection")
    public String listSampleCollection(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        List<CRUDsampleCollectionResponse> sampleCollections = staffService.getSampleCollectionTasks(currentPrincipalName);
        model.addAttribute("sampleCollections", sampleCollections);
        model.addAttribute("section", "sample-collection");
        return "staff/list-sample-collection";
    }
    @GetMapping("/details-sample-collection")
    public String detailsSampleCollection(@RequestParam("sampleCollectionId") Long sampleCollectionId, Model model) {
        CRUDsampleCollectionResponse sampleCollection = staffService.getSampleCollectionTasksById(sampleCollectionId);
        model.addAttribute("sampleCollection", sampleCollection);
        model.addAttribute("section", "sample-collection");
        return "staff/details-sample-collection"; // Assuming you have a Thymeleaf template named "details-sample-collection.html"
    }


    @GetMapping("/update-sample-collection")
    public String viewUpdateSampleCollection(@RequestParam("sampleCollectionId") Long sampleCollectionId, Model model) {
        CRUDsampleCollectionResponse sampleCollection = staffService.getSampleCollectionTasksById(sampleCollectionId);
        model.addAttribute("sampleCollection", sampleCollection);
        model.addAttribute("section", "sample-collection");
        return "staff/update-sample-collection";
    }
    @PostMapping("/update-sample-collection")
    public String updateSampleCollection(
            @RequestParam("sampleCollectionId") Long sampleCollectionId,
            @RequestParam("collectionStatus") String collectionStatus,
            @RequestParam("sampleQuality") String sampleQuality,
            RedirectAttributes redirectAttributes) {

        try {
            // Validate input
            if (sampleCollectionId == null || collectionStatus == null || sampleQuality == null) {
                throw new IllegalArgumentException("Missing required parameters");
            }

            staffService.updateSampleCollectionStatus(sampleCollectionId, collectionStatus, sampleQuality);
            redirectAttributes.addFlashAttribute("successMessage", "Sample collection updated successfully!");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid input: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update: " + e.getMessage());
        }

        return "redirect:/staff/list-sample-collection";
    }

    @GetMapping("/list-test-results")
    public String listTestResults(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        List<TestResultsResponse> testResults = staffService.getTestResults(currentPrincipalName);
        model.addAttribute("testResults", testResults);
        model.addAttribute("section", "test-results");
        return "staff/list-test-results"; // Assuming you have a Thymeleaf template named "list-test-results.html"
    }

    @GetMapping("/details-test-result")
    public String detailsTestResult(@RequestParam("testResultId") Long testResultId, Model model) {
        TestResultsResponse testResult = staffService.getTestResultById(testResultId);
        RawDataResponse rawData = staffService.getRawDataById(testResult.getRawTestId());
        model.addAttribute("testResult", testResult);
        model.addAttribute("rawData", rawData);
        model.addAttribute("section", "test-results");
        return "staff/details-test-result"; // Assuming you have a Thymeleaf template named "details-test-result.html"
    }

    @GetMapping("/update-test-result")
    public String viewUpdateTestResult(@RequestParam("testResultId") Long testResultId, Model model) {
        TestResultsResponse testResult = staffService.getTestResultById(testResultId);
        model.addAttribute("testResult", testResult);
        model.addAttribute("section", "test-results");
        return "staff/update-test-result";
    }

    @PostMapping("/update-test-result")
    public String updateTestResult(@RequestParam("testResultId") Long testResultId,
                                   @RequestParam("reportFilePath") MultipartFile reportFilePath,
                                   @ModelAttribute("testResultEdit") TestResultsResquest testResultsResquest
                                   ) {
        TestResultsResponse existingTestResult = staffService.getTestResultById(testResultId);
        if( reportFilePath == null || reportFilePath.isEmpty() && existingTestResult.getReportFile() != null) {
            testResultsResquest.setReportFile(existingTestResult.getReportFile());
        }
        else{
            if (existingTestResult.getReportFile() == null ) {
                testResultsResquest.setReportFile(fileEdit.editFile(reportFilePath));
            }
            else {
                fileEdit.deleteFile(existingTestResult.getReportFile());
                testResultsResquest.setReportFile(fileEdit.editFile(reportFilePath));
            }
        }
        if (testResultsResquest.getTestDate() == null) {
            testResultsResquest.setTestDate(existingTestResult.getTestDate()); // Set current date if null
        }
        testResultsResquest.setReportGenerated(true);
        staffService.updateTestResult(testResultsResquest, testResultId);
        return "redirect:/staff/list-test-results"; // Redirect to the list of test results after updating
    }
    // end update test result
    // update raw data
    @GetMapping("/update-raw-data")
    public String listRawData(@RequestParam("testResultId") Long testResultId, Model model) {
        TestResultsResponse testResult = staffService.getTestResultById(testResultId);
        RawDataResponse rawData = staffService.getRawDataById(testResult.getRawTestId());
        model.addAttribute("rawData", rawData);
        model.addAttribute("today", LocalDateTime.now());
        model.addAttribute("section", "test-results");
        return "staff/update-raw-data";
    }

    @PostMapping("/update-raw-data")
    public String updateRawData(@RequestParam("rawDataId") Long rawDataId,
                                @RequestParam("filePath") MultipartFile filePath,
                                @ModelAttribute("rawDataEdit") RawDataRequest rawDataRequest) {
        RawDataResponse existingRawData = staffService.getRawDataById(rawDataId);
        if (filePath == null || filePath.isEmpty() && existingRawData.getFilePath() != null) {
            rawDataRequest.setFile(existingRawData.getFilePath());
        }
        else {
            if (existingRawData.getFilePath() == null) {
                rawDataRequest.setFile(fileEdit.editFile(filePath));
            } else {
                fileEdit.deleteFile(existingRawData.getFilePath());
                rawDataRequest.setFile(fileEdit.editFile(filePath));
            }
        }
        if( rawDataRequest.getCollectionDate() == null) {
            rawDataRequest.setCollectionDate(existingRawData.getCollectedAt()); // Set current date if null
        }
        staffService.updateRawData(rawDataRequest, rawDataId);
        return "redirect:/staff/list-test-results"; // Redirect to the list of test results after updating
    }
    // end update raw data

    @GetMapping("/search-orders")
    public String searchOrders(@RequestParam("query") Long query, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        CRUDorderResponse serviceOrders = staffService.searchOrderById(query, currentPrincipalName);
        if(serviceOrders == null) {
            model.addAttribute("errorMessage", "No order found with ID: " + query);
            return "redirect:/staff/list-orders"; // Redirect to the list of orders if no order is found
        }
        model.addAttribute("serviceOrders", serviceOrders);
        return "staff/list-orders"; // Assuming you have a Thymeleaf template named "list-orders.html"
    }

    @GetMapping("/search-test-results")
    public String searchTestResults(@RequestParam("query") Long query, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        TestResultsResponse testResults = staffService.searchTestResultById(query, currentPrincipalName);
        if(testResults == null) {
            model.addAttribute("errorMessage", "No test result found with ID: " + query);
            return "redirect:/staff/list-test-results"; // Redirect to the list of test results if no result is found
        }
        model.addAttribute("testResults", testResults);
        return "staff/list-test-results"; // Assuming you have a Thymeleaf template named "list-test-results.html"
    }

    @GetMapping("/search-sample-collections")
    public String searchSampleCollections(@RequestParam("query") Long query, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        CRUDsampleCollectionResponse sampleCollections = staffService.searchSampleCollectionById(query, currentPrincipalName);
        if(sampleCollections == null) {
            model.addAttribute("errorMessage", "No sample collection found with ID: " + query);
            return "redirect:/staff/list-sample-collection"; // Redirect to the list of sample collections if no collection is found
        }
        model.addAttribute("sampleCollections", sampleCollections);
        return "staff/list-sample-collection"; // Assuming you have a Thymeleaf template named "list-sample-collection.html"
    }

    @GetMapping("/filter-orders")
    public String filterOrders(@RequestParam("status") String status, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        List<CRUDorderResponse> filteredOrders = staffService.filterOrdersByStatus(status, currentPrincipalName);
        if(filteredOrders == null) {
            model.addAttribute("errorMessage", "No orders found with status: " + status);
            return "redirect:/staff/list-orders"; // Redirect to the list of orders if no orders are found
        }
        model.addAttribute("serviceOrders", filteredOrders);
        return "staff/list-orders"; // Assuming you have a Thymeleaf template named "list-orders.html"
    }

    @GetMapping("/filter-sample-collections")
    public String filterSampleCollections(@RequestParam("status") String status, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        List<CRUDsampleCollectionResponse> filteredSampleCollections = staffService.filterSampleCollectionsByStatus(status, currentPrincipalName);
        model.addAttribute("sampleCollections", filteredSampleCollections);
        return "staff/list-sample-collection"; // Assuming you have a Thymeleaf template named "list-sample-collection.html"
    }

    @GetMapping("/filter-test-results")
    public String filterTestResults(@RequestParam("status") String status, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        List<TestResultsResponse> filteredTestResults = staffService.filterTestResultsByStatus(status, currentPrincipalName);
        model.addAttribute("testResults", filteredTestResults);
        return "staff/list-test-results"; // Assuming you have a Thymeleaf template named "list-test-results.html"
    }


    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "section", required = false) String section, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        if ("orders".equals(section)) {
            List<CRUDorderResponse> serviceOrders = staffService.getForStaff(username);
            model.addAttribute("serviceOrders", serviceOrders);
        } else if ("sample-collection".equals(section)) {
            List<CRUDsampleCollectionResponse> sampleCollections = staffService.getSampleCollectionTasks(username);
            model.addAttribute("sampleCollections", sampleCollections);
        } else if ("test-results".equals(section)) {
            List<TestResultsResponse> testResults = staffService.getTestResults(username);
            model.addAttribute("testResults", testResults);
        } else if ("payment-management".equals(section)) {
            // Redirect to payment management page
            return "redirect:/staff/payment-management";
        }

        model.addAttribute("section", section);
        return "staff/dashboard";
    }

    // Payment Management Methods
    @GetMapping("/payment-management")
    public String paymentManagement(@RequestParam(value = "query", required = false) String query,
                                  @RequestParam(value = "status", required = false) String status,
                                  @RequestParam(value = "sortBy", required = false) String sortBy,
                                  @RequestParam(value = "sortDir", required = false) String sortDir,
                                  Model model) {
        List<PaymentResponse> payments = paymentService.getPayments();
        
        // Filter by search query if provided
        if (query != null && !query.trim().isEmpty()) {
            payments = payments.stream()
                    .filter(payment -> 
                        payment.getId().toString().contains(query) ||
                        (payment.getOrderId() != null && payment.getOrderId().toString().contains(query)) ||
                        (payment.getCustomerName() != null &&
                         payment.getCustomerName().toLowerCase().contains(query.toLowerCase())) ||
                        (payment.getEmail() != null &&
                         payment.getEmail().toLowerCase().contains(query.toLowerCase())))
                    .collect(Collectors.toList());
        }
        
        // Filter by status if provided
        if (status != null && !status.trim().isEmpty()) {
            try {
                PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
                payments = payments.stream()
                        .filter(payment -> payment.getPaymentStatus() != null && 
                                payment.getPaymentStatus().equalsIgnoreCase(paymentStatus.name()))
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore filter
            }
        }
        
        // Sort payments if requested
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            boolean ascending = "asc".equalsIgnoreCase(sortDir);
            payments = sortPayments(payments, sortBy, ascending);
        }
        
        // Calculate statistics based on all payments (not filtered)
        List<PaymentResponse> allPayments = paymentService.getPayments();
        Map<String, Long> stats = calculatePaymentStatistics(allPayments);
        
        // Add pagination and breadcrumbs
        model.addAttribute("payments", payments);
        model.addAttribute("totalPayments", stats.get("total"));
        model.addAttribute("pendingPayments", stats.get("pending"));
        model.addAttribute("completedPayments", stats.get("completed"));
        model.addAttribute("failedPayments", stats.get("failed"));
        model.addAttribute("section", "payment-management");
        model.addAttribute("pageTitle", "Payment Management");
        model.addAttribute("breadcrumbs", createBreadcrumbs("Payment Management"));
        
        return "staff/payment-management";
    }
    
    @PostMapping("/payment-management/update-status")
    public String updatePaymentStatus(@RequestParam Long paymentId,
                                    @RequestParam String paymentStatus,
                                    @RequestParam(required = false) String notes,
                                    RedirectAttributes redirectAttributes) {
        try {
            PaymentUpdatingRequest request = new PaymentUpdatingRequest();
            request.setPaymentId(paymentId);
            request.setPaymentStatus(paymentStatus);
            
            paymentService.updatePaymentStatus(request);
            
            redirectAttributes.addAttribute("success", "true");
            log.info("Payment status updated successfully for payment ID: {} to status: {}", paymentId, paymentStatus);
        } catch (Exception e) {
            log.error("Error updating payment status: {}", e.getMessage());
            redirectAttributes.addAttribute("error", "true");
        }
        
        return "redirect:/staff/payment-management";
    }
    
    private Map<String, Long> calculatePaymentStatistics(List<PaymentResponse> payments) {
        Map<String, Long> stats = new HashMap<>();
        
        stats.put("total", (long) payments.size());
        stats.put("pending", payments.stream()
                .filter(p -> p.getPaymentStatus() != null && 
                        p.getPaymentStatus().equalsIgnoreCase(PaymentStatus.PENDING.name()))
                .count());
        stats.put("completed", payments.stream()
                .filter(p -> p.getPaymentStatus() != null && 
                        p.getPaymentStatus().equalsIgnoreCase(PaymentStatus.PAID.name()))
                .count());
        return stats;
    }
    
    private List<PaymentResponse> sortPayments(List<PaymentResponse> payments, String sortBy, boolean ascending) {
        Stream<PaymentResponse> stream = payments.stream();
        
        switch (sortBy) {
            case "customerName":
                stream = stream.sorted((p1, p2) -> {
                    String name1 = p1.getCustomerName() != null ? p1.getCustomerName() : "";
                    String name2 = p2.getCustomerName() != null ? p2.getCustomerName() : "";
                    return ascending ? name1.compareTo(name2) : name2.compareTo(name1);
                });
                break;
            case "netAmount":
                stream = stream.sorted((p1, p2) -> {
                    BigDecimal amount1 = p1.getNetAmount() != null ? p1.getNetAmount() : BigDecimal.ZERO;
                    BigDecimal amount2 = p2.getNetAmount() != null ? p2.getNetAmount() : BigDecimal.ZERO;
                    return ascending ? amount1.compareTo(amount2) : amount2.compareTo(amount1);
                });
                break;
            case "paymentStatus":
                stream = stream.sorted((p1, p2) -> {
                    String status1 = p1.getPaymentStatus() != null ? p1.getPaymentStatus() : "";
                    String status2 = p2.getPaymentStatus() != null ? p2.getPaymentStatus() : "";
                    return ascending ? status1.compareTo(status2) : status2.compareTo(status1);
                });
                break;
            case "paymentMethod":
                stream = stream.sorted((p1, p2) -> {
                    String method1 = p1.getPaymentMethod() != null ? p1.getPaymentMethod() : "";
                    String method2 = p2.getPaymentMethod() != null ? p2.getPaymentMethod() : "";
                    return ascending ? method1.compareTo(method2) : method2.compareTo(method1);
                });
                break;
            case "createdAt":
            default:
                stream = stream.sorted((p1, p2) -> {
                    LocalDateTime date1 = p1.getCreatedAt() != null ? p1.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime date2 = p2.getCreatedAt() != null ? p2.getCreatedAt() : LocalDateTime.MIN;
                    return ascending ? date1.compareTo(date2) : date2.compareTo(date1);
                });
                break;
        }
        
        return stream.collect(Collectors.toList());
    }
    
    private List<Map<String, Object>> createBreadcrumbs(String currentPage) {
        List<Map<String, Object>> breadcrumbs = new ArrayList<>();
        
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("name", "Dashboard");
        dashboard.put("url", "/staff/dashboard");
        dashboard.put("active", false);
        breadcrumbs.add(dashboard);
        
        Map<String, Object> current = new HashMap<>();
        current.put("name", currentPage);
        current.put("url", "");
        current.put("active", true);
        breadcrumbs.add(current);
        
        return breadcrumbs;
    }
}
