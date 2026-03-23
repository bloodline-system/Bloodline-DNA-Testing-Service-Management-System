package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.manager.StaffAssignmentRequest;
import com.dna_testing_system.dev.dto.request.manager.UpdateOrderStatusRequest;
import com.dna_testing_system.dev.dto.request.StaffAvailableRequest;
import com.dna_testing_system.dev.dto.response.manager.OrderManagementResponse;
import com.dna_testing_system.dev.dto.response.manager.NewOrdersResponse;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager")
public class ManagerOrderController {
    private final OrderTaskManagementService orderTaskManagementService;

    @PostMapping("/order-management/update-status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updateOrderStatus(@Valid @RequestBody UpdateOrderStatusRequest request) {
        orderTaskManagementService.updateOrderStatus(request.getOrderId(), request.getStatus());
        return ApiResponse.success(HttpStatus.OK.value(), "Order status updated", null);
    }

    @GetMapping("/order-management")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<OrderManagementResponse> orderManagement() {
        var orders = orderTaskManagementService.getServiceOrders();

        Map<String, Long> statusCounts = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getOrderStatus().name().toLowerCase().replace("_", "-"),
                        Collectors.counting()));

        OrderManagementResponse response = OrderManagementResponse.builder()
                .orders(orders)
                .pageTitle("Order Management")
                .statusCounts(statusCounts)
                .totalOrders(orders.size())
                .build();

        return ApiResponse.success(HttpStatus.OK.value(), "Order management data", response);
    }

    @GetMapping("/new-orders")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<NewOrdersResponse> newOrders() {
        var newOrders = orderTaskManagementService.getNewOrders();
        var availableStaff = orderTaskManagementService.getStaffAvailable();

        NewOrdersResponse response = NewOrdersResponse.builder()
                .newOrders(newOrders)
                .availableStaff(availableStaff)
                .newOrdersCount(newOrders.size())
                .availableStaffCount(availableStaff.size())
                .assignedTodayCount(0)
                .pendingAssignmentCount(newOrders.size())
                .build();

        return ApiResponse.success(HttpStatus.OK.value(), "New orders data", response);
    }

    @PostMapping("/assign-staff")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> assignStaff(@Valid @RequestBody StaffAssignmentRequest request) {
        StaffAvailableRequest collectStaffRequest = StaffAvailableRequest.builder()
                .staffId(request.getCollectStaffId())
                .build();
        StaffAvailableRequest analysisStaffRequest = StaffAvailableRequest.builder()
                .staffId(request.getAnalysisStaffId())
                .build();

        orderTaskManagementService.taskAssignmentForStaff(request.getOrderId(), collectStaffRequest, analysisStaffRequest);
        return ApiResponse.success(HttpStatus.OK.value(), "Staff assigned to order", null);
    }
}
