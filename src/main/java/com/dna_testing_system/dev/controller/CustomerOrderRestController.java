package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.OrderTestKitRequest;
import com.dna_testing_system.dev.dto.request.ParticipantRequest;
import com.dna_testing_system.dev.dto.request.ServiceOrderRequestByCustomer;
import com.dna_testing_system.dev.dto.response.*;
import com.dna_testing_system.dev.dto.response.medical_service.MedicalServiceResponse;
import com.dna_testing_system.dev.dto.response.test_kit.TestKitResponse;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.enums.CollectionType;
import com.dna_testing_system.dev.service.*;
import com.dna_testing_system.dev.service.service.MedicalServiceManageService;
import com.dna_testing_system.dev.service.staff.TestKitService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/customer")
public class CustomerOrderRestController {

    OrderService orderService;
    OrderKitService orderKitService;
    OrderParticipantService orderParticipantService;
    MedicalServiceManageService medicalService;
    TestKitService testKitService;
    UserProfileService userProfileService;
    CustomerFeedbackService customerFeedbackService;

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MEDICAL SERVICES
    // GET /user/list-service → GET /api/v1/customer/services
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<MedicalServiceResponse>>> listMedicalServices() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            userProfileService.getUserProfile(authentication.getName());
        }

        List<MedicalServiceResponse> services = medicalService.getAllServices();
        return ResponseEntity.ok(
                ApiResponse.success(200, "Services retrieved successfully", services)
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CREATE ORDER
    // GET  /user/order-service → GET  /api/v1/customer/orders/form-data
    // POST /user/order-service → POST /api/v1/customer/orders
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/orders/form-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderFormData(
            @RequestParam("medicalServiceId") Long medicalServiceId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Map<String, Object> formData = new HashMap<>();
        formData.put("medicalServiceId", medicalServiceId);
        formData.put("collectionTypes", CollectionType.values());
        formData.put("username", username);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Form data retrieved successfully", formData)
        );
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<ServiceOrderByCustomerResponse>> createOrder(
            @RequestBody ServiceOrderRequestByCustomer request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        request.setUsername(username);
        request.setCreatedBy(username);

        ServiceOrderByCustomerResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Order created successfully", response)
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PARTICIPANTS
    // GET  /user/participant-information → GET  /api/v1/customer/orders/{id}/participants
    // POST /user/participant-information → POST /api/v1/customer/orders/{id}/participants
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/orders/{id}/participants")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getParticipants(
            @PathVariable Long id
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            userProfileService.getUserProfile(authentication.getName());
        }

        ServiceOrder serviceOrder = orderService.getOrderByIdEntity(id);
        MedicalServiceResponse medicalServiceResponse =
                medicalService.getServiceById(serviceOrder.getService().getId());
        List<OrderParticipantResponse> participants =
                orderParticipantService.getAllParticipantsByOrderId(id);
        int count = (participants != null && !participants.isEmpty()) ? participants.size() : 0;

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", id);
        data.put("medicalService", medicalServiceResponse);
        data.put("participants", participants);
        data.put("participantCount", count);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Participants retrieved successfully", data)
        );
    }

    @PostMapping("/orders/{id}/participants")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addParticipant(
            @PathVariable Long id,
            @RequestBody ParticipantRequest participantRequest
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            userProfileService.getUserProfile(authentication.getName());
        }

        orderParticipantService.createOrderParticipant(id, participantRequest);

        ServiceOrder serviceOrder = orderService.getOrderByIdEntity(id);
        MedicalServiceResponse medicalServiceResponse =
                medicalService.getServiceById(serviceOrder.getService().getId());
        List<OrderParticipantResponse> participants =
                orderParticipantService.getAllParticipantsByOrderId(id);
        int count = (participants != null && !participants.isEmpty()) ? participants.size() : 0;

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", id);
        data.put("medicalService", medicalServiceResponse);
        data.put("participants", participants);
        data.put("participantCount", count);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Participant added successfully", data)
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST KITS
    // GET /user/list-kit  → GET /api/v1/customer/kits
    // GET /user/order-kit → GET /api/v1/customer/kits/{kitId}
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/kits")
    public ResponseEntity<ApiResponse<List<TestKitResponse>>> listTestKits(
            @RequestParam("orderId") Long orderId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            userProfileService.getUserProfile(authentication.getName());
        }

        List<TestKitResponse> testKits = testKitService.GetTestKitResponseList();
        return ResponseEntity.ok(
                ApiResponse.success(200, "Test kits retrieved successfully", testKits)
        );
    }

    @GetMapping("/kits/{kitId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTestKitDetail(
            @PathVariable Long kitId,
            @RequestParam("orderId") Long orderId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            userProfileService.getUserProfile(authentication.getName());
        }

        TestKitResponse testKit = testKitService.GetTestKitResponseById(kitId);

        Map<String, Object> data = new HashMap<>();
        data.put("kitTestId", kitId);
        data.put("orderId", orderId);
        data.put("testKit", testKit);
        data.put("maxQuantity", testKit.getQuantityInStock());

        return ResponseEntity.ok(
                ApiResponse.success(200, "Test kit retrieved successfully", data)
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ADD KIT TO ORDER
    // POST /user/order-kit → POST /api/v1/customer/orders/{id}/kits
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/orders/{id}/kits")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addKitToOrder(
            @PathVariable Long id,
            @RequestBody OrderTestKitRequest orderTestKitRequest
    ) {
        orderTestKitRequest.setOrderId(id);
        orderKitService.createOrder(id, orderTestKitRequest);

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", id);
        data.put("message", "Order placed successfully!");

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Kit added to order successfully", data)
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ORDER DETAIL
    // GET /user/detail → GET /api/v1/customer/orders/{id}
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderDetail(
            @PathVariable Long id
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            userProfileService.getUserProfile(authentication.getName());
        }

        ServiceOrderByCustomerResponse detail = orderService.getOrderById(id);
        List<OrderTestKitResponse> testKitDetails = orderKitService.getOrderById(id);
        List<OrderParticipantResponse> participantDetails =
                orderParticipantService.getAllParticipantsByOrderId(id);

        BigDecimal paymentTotal = BigDecimal.ZERO;
        for (OrderTestKitResponse kit : testKitDetails) {
            paymentTotal = paymentTotal.add(kit.getTotalPrice());
        }
        paymentTotal = paymentTotal.add(detail.getFinalAmount());

        Map<String, Object> data = new HashMap<>();
        data.put("detail", detail);
        data.put("testKitDetails", testKitDetails);
        data.put("participantDetails", participantDetails);
        data.put("paymentTotal", paymentTotal);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Order retrieved successfully", data)
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ORDER HISTORY
    // GET /user/order-history → GET /api/v1/customer/orders
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<ServiceOrderByCustomerResponse>>> getOrderHistory() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            List<ServiceOrderByCustomerResponse> orders =
                    orderService.getAllOrdersByCustomerId(authentication.getName());
            return ResponseEntity.ok(
                    ApiResponse.success(200, "Orders retrieved successfully", orders)
            );
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.error(401, "Unauthorized", null)
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CANCEL ORDER
    // POST /user/cancel → POST /api/v1/customer/orders/{id}/cancel
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id) {
        try {
            orderService.cancelOrder(id);
            return ResponseEntity.ok(
                    ApiResponse.success(200, "Order cancelled successfully", null)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Failed to cancel order: " + e.getMessage(), null)
            );
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ACCEPT ORDER
    // POST /user/accept → POST /api/v1/customer/orders/{id}/accept
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/orders/{id}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptOrder(@PathVariable Long id) {
        try {
            orderService.acceptOrder(id);
            return ResponseEntity.ok(
                    ApiResponse.success(200, "Order accepted successfully", null)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Failed to accept order: " + e.getMessage(), null)
            );
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ORDER DETAILS WITH FEEDBACK
    // GET /user/order-details → GET /api/v1/customer/orders/{id}/details
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/orders/{id}/details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderDetails(
            @PathVariable Long id
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserProfileResponse userProfile = null;

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            userProfile = userProfileService.getUserProfile(authentication.getName());
        }

        ServiceOrderByCustomerResponse orderDetails = orderService.getOrderById(id);
        List<OrderTestKitResponse> orderTestKits = orderKitService.getOrderById(id);
        List<OrderParticipantResponse> orderParticipants =
                orderParticipantService.getAllParticipantsByOrderId(id);

        BigDecimal paymentTotal = BigDecimal.ZERO;
        for (OrderTestKitResponse kit : orderTestKits) {
            paymentTotal = paymentTotal.add(kit.getTotalPrice());
        }
        paymentTotal = paymentTotal.add(orderDetails.getFinalAmount());

        // Lấy feedback nếu đơn đã COMPLETED
        CustomerFeedbackResponse existingFeedback = null;
        if ("COMPLETED".equals(orderDetails.getOrderStatus().name()) && userProfile != null) {
            try {
                String currentUserId = userProfile.getUserId();
                log.info("Looking for feedback for order {} and user {}", id, currentUserId);

                List<CustomerFeedbackResponse> userFeedbacks =
                        customerFeedbackService.getFeedbackByCustomer(currentUserId);
                log.info("Found {} total feedbacks for user {}", userFeedbacks.size(), currentUserId);

                existingFeedback = userFeedbacks.stream()
                        .filter(f -> f != null && id.equals(f.getOrderId()))
                        .findFirst()
                        .orElse(null);

                if (existingFeedback != null) {
                    log.info("Found existing feedback for order {}: title={}, hasResponse={}",
                            id, existingFeedback.getFeedbackTitle(),
                            existingFeedback.getResponseContent() != null);
                } else {
                    log.info("No existing feedback found for order {}", id);
                }
            } catch (Exception e) {
                log.error("Error checking for existing feedback for order: {}", id, e);
            }
        } else {
            log.info("Order {} status: {}, User authenticated: {}",
                    id, orderDetails.getOrderStatus(), userProfile != null);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("orderDetails", orderDetails);
        data.put("orderTestKits", orderTestKits);
        data.put("orderParticipants", orderParticipants);
        data.put("paymentTotal", paymentTotal);
        data.put("existingFeedback", existingFeedback);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Order details retrieved successfully", data)
        );
    }
}