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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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

    // ================== DNÁ ORDERS COMPATIBILITY ENDPOINTS (FOR POSTMAN QA) ==================

    @GetMapping("/dna-orders")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> getDnaOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0") int dummy) {
        var orders = orderTaskManagementService.getServiceOrders();

        int totalOrders = orders.size();
        int start = Math.min(page * size, totalOrders);
        int end = Math.min(start + size, totalOrders);

        var pageOrders = orders.subList(start, end);

        long completedOrders = orders.stream().filter(o -> o.getOrderStatus() == com.dna_testing_system.dev.enums.ServiceOrderStatus.COMPLETED).count();
        long cancelledOrders = orders.stream().filter(o -> o.getOrderStatus() == com.dna_testing_system.dev.enums.ServiceOrderStatus.CANCELLED).count();

        var summary = Map.<String, Object>of(
                "totalOrders", totalOrders,
                "completedOrders", completedOrders,
                "cancelledOrders", cancelledOrders
        );

        var responseObj = Map.<String, Object>of(
                "orders", pageOrders,
                "summary", summary,
                "currentPage", page,
                "pageSize", size,
                "totalPages", (int) Math.ceil((double) totalOrders / size),
                "totalOrders", totalOrders,
                "filters", Map.of("status", "all", "page", page, "size", size));

        return ApiResponse.success(HttpStatus.OK.value(), "DNA orders fetched", responseObj);
    }

    @GetMapping("/dna-orders/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> getDnaOrderDetails(@PathVariable Long orderId) {
        var order = orderTaskManagementService.getServiceOrders().stream()
                .filter(o -> orderId.equals(o.getId()))
                .findFirst()
                .orElseThrow(() -> new com.dna_testing_system.dev.exception.ResourceNotFoundException(com.dna_testing_system.dev.exception.ErrorCode.NOT_FOUND));

        var responseObj = Map.<String, Object>of(
                "order", order,
                "kits", order.getOrderKits() == null ? List.of() : order.getOrderKits(),
                "participants", order.getOrderParticipants() == null ? List.of() : order.getOrderParticipants());

        return ApiResponse.success(HttpStatus.OK.value(), "DNA order detail", responseObj);
    }

    @PatchMapping("/dna-orders/{orderId}/status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updateDnaOrderStatus(@PathVariable Long orderId,
            @Valid @RequestBody com.dna_testing_system.dev.dto.request.manager.UpdateOrderStatusRequest request) {
        orderTaskManagementService.updateOrderStatus(orderId, request.getStatus());
        return ApiResponse.success(HttpStatus.OK.value(), "Order status updated", null);
    }

    @PostMapping("/dna-orders/{orderId}/assign")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> assignDnaOrderStaff(@PathVariable Long orderId,
            @Valid @RequestBody com.dna_testing_system.dev.dto.request.manager.StaffAssignmentRequest request) {
        request.setOrderId(orderId);
        orderTaskManagementService.taskAssignmentForStaff(orderId,
                com.dna_testing_system.dev.dto.request.StaffAvailableRequest.builder().staffId(request.getCollectStaffId()).build(),
                com.dna_testing_system.dev.dto.request.StaffAvailableRequest.builder().staffId(request.getAnalysisStaffId()).build());
        return ApiResponse.success(HttpStatus.OK.value(), "Staff assigned", null);
    }

    @GetMapping("/dna-orders/pending-assignment")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Object> getDnaPendingAssignment() {
        var newOrders = orderTaskManagementService.getNewOrders();
        var availableStaff = orderTaskManagementService.getStaffAvailable();

        var responseObj = Map.<String, Object>of(
                "pendingOrders", newOrders,
                "availableStaff", availableStaff,
                "pendingCount", newOrders.size(),
                "availableStaffCount", availableStaff.size());

        return ApiResponse.success(HttpStatus.OK.value(), "DNA pending assignment data", responseObj);
    }

    // =====================================================================================

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
