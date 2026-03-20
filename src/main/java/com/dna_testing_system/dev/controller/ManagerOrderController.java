package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.StaffAvailableRequest;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManagerOrderController {
    OrderTaskManagementService orderTaskManagementService;

    @PostMapping("/order-management/update-status")
    public ApiResponse<Void> updateOrderStatus(@RequestParam Long orderId,
            @RequestParam String status,
            @RequestParam(required = false) String notes) {
        try {
            orderTaskManagementService.updateOrderStatus(orderId, status);
            log.info("Order status updated successfully for order ID: {}", orderId);
            return ApiResponse.success(HttpStatus.OK.value(), "Order status updated", null);
        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage());
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to update order status",
                    "/api/v1/manager/order-management/update-status");
        }
    }

    @GetMapping("/order-management")
    public ApiResponse<Map<String, Object>> orderManagement() {
        Map<String, Object> response = new HashMap<>();

        try {
            var orders = orderTaskManagementService.getServiceOrders();
            response.put("orders", orders);
            response.put("pageTitle", "Order Management");

            // Calculate counts for each status
            Map<String, Long> statusCounts = orders.stream()
                    .collect(Collectors.groupingBy(
                            order -> order.getOrderStatus().name().toLowerCase().replace("_", "-"),
                            Collectors.counting()));

            response.put("statusCounts", statusCounts);
            response.put("totalOrders", orders.size());
        } catch (Exception e) {
            log.error("Error loading orders: {}", e.getMessage());
            response.put("orders", new ArrayList<>());
            response.put("statusCounts", new HashMap<>());
            response.put("totalOrders", 0);
        }

        return ApiResponse.success(HttpStatus.OK.value(), "Order management data", response);
    }

    @GetMapping("/new-orders")
    public ApiResponse<Map<String, Object>> newOrders() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Load new orders (unassigned orders with PENDING status)
            var newOrders = orderTaskManagementService.getNewOrders();

            // Load available staff
            var availableStaff = orderTaskManagementService.getStaffAvailable();

            response.put("newOrders", newOrders);
            response.put("availableStaff", availableStaff);
            response.put("newOrdersCount", newOrders.size());
            response.put("availableStaffCount", availableStaff.size());
            response.put("assignedTodayCount", 0); // This could be calculated from database
            response.put("pendingAssignmentCount", newOrders.size());

            return ApiResponse.success(HttpStatus.OK.value(), "New orders data", response);
        } catch (Exception e) {
            log.error("Error loading new orders: {}", e.getMessage());
            response.put("newOrders", new ArrayList<>());
            response.put("availableStaff", new ArrayList<>());
            response.put("newOrdersCount", 0);
            response.put("availableStaffCount", 0);
            response.put("assignedTodayCount", 0);
            response.put("pendingAssignmentCount", 0);
            return ApiResponse.success(HttpStatus.OK.value(), "New orders data", response);
        }
    }

    @PostMapping("/assign-staff")
    public ApiResponse<Void> assignStaff(@RequestParam Long orderId,
            @RequestParam String collectStaffId,
            @RequestParam String analysisStaffId,
            @RequestParam(required = false) String assignmentType,
            @RequestParam(required = false) String notes) {
        try {
            StaffAvailableRequest collectStaffRequest = StaffAvailableRequest.builder()
                    .staffId(collectStaffId)
                    .build();
            StaffAvailableRequest analysisStaffRequest = StaffAvailableRequest.builder()
                    .staffId(analysisStaffId)
                    .build();

            orderTaskManagementService.taskAssignmentForStaff(orderId, collectStaffRequest, analysisStaffRequest);
            return ApiResponse.success(HttpStatus.OK.value(), "Staff assigned to order", null);
        } catch (Exception e) {
            log.error("Failed to assign staff: {}", e.getMessage());
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to assign staff",
                    "/api/v1/manager/assign-staff");
        }
    }
}
