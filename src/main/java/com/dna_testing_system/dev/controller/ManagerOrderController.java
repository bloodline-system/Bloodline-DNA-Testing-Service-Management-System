package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.StaffAvailableRequest;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.exception.EntityNotFoundException;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/manager/orders")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('MANAGER')")
public class ManagerOrderController {

    OrderTaskManagementService orderTaskManagementService;
    UserRepository userRepository;

    /**
     * GET /api/v1/manager/orders
     * Retrieve all service orders with status counts
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllOrders(
            HttpServletRequest request) {

        try {
            // Authenticate user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Not authenticated", request.getRequestURI()));
            }
            User currentUser = userRepository.findByUsername(auth.getName())
                    .map(u -> userRepository.findById(u.getId())
                            .orElseThrow(() -> new RuntimeException("User not found")))
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));

            var orders = orderTaskManagementService.getServiceOrders();

            // Calculate counts for each status
            Map<String, Long> statusCounts = orders.stream()
                    .collect(Collectors.groupingBy(
                            order -> order.getOrderStatus().name().toLowerCase().replace("_", "-"),
                            Collectors.counting()));

            Map<String, Object> data = Map.of(
                    "orders", orders,
                    "statusCounts", statusCounts,
                    "totalOrders", orders.size(),
                    "pageTitle", "Order Management"
            );

            log.info("Order list loaded by user: {}", currentUser.getUsername());
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Order list loaded", data));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not found", request.getRequestURI()));
        } catch (Exception e) {
            log.error("Error loading orders: ", e);
            Map<String, Object> errorData = Map.of(
                    "orders", new ArrayList<>(),
                    "statusCounts", new HashMap<>(),
                    "totalOrders", 0
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to load orders data", request.getRequestURI()));
        }
    }

    /**
     * GET /api/v1/manager/orders/new
     * Retrieve new/unassigned orders with available staff
     */
    @GetMapping("/new")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNewOrders(
            HttpServletRequest request) {

        try {
            // Authenticate user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Not authenticated", request.getRequestURI()));
            }
            User currentUser = userRepository.findByUsername(auth.getName())
                    .map(u -> userRepository.findById(u.getId())
                            .orElseThrow(() -> new RuntimeException("User not found")))
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));

            // Load new orders (unassigned orders with PENDING status)
            var newOrders = orderTaskManagementService.getNewOrders();
            // Load available staff
            var availableStaff = orderTaskManagementService.getStaffAvailable();

            Map<String, Object> data = Map.of(
                    "newOrders", newOrders,
                    "availableStaff", availableStaff,
                    "newOrdersCount", newOrders.size(),
                    "availableStaffCount", availableStaff.size(),
                    "assignedTodayCount", 0,
                    "pendingAssignmentCount", newOrders.size()
            );

            log.info("New orders loaded by user: {}", currentUser.getUsername());
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "New orders data", data));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not found", request.getRequestURI()));
        } catch (Exception e) {
            log.error("Error loading new orders: ", e);
            Map<String, Object> errorData = Map.of(
                    "newOrders", new ArrayList<>(),
                    "availableStaff", new ArrayList<>(),
                    "newOrdersCount", 0,
                    "availableStaffCount", 0,
                    "assignedTodayCount", 0,
                    "pendingAssignmentCount", 0
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unable to load new orders data", request.getRequestURI()));
        }
    }

    /**
     * PATCH /api/v1/manager/orders/{orderId}/status
     * Update order status
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            @RequestParam(required = false) String notes,
            HttpServletRequest request) {

        try {
            // Authenticate user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Not authenticated", request.getRequestURI()));
            }
            User currentUser = userRepository.findByUsername(auth.getName())
                    .map(u -> userRepository.findById(u.getId())
                            .orElseThrow(() -> new RuntimeException("User not found")))
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));

            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Status cannot be empty", request.getRequestURI()));
            }

            orderTaskManagementService.updateOrderStatus(orderId, status);

            Map<String, Object> data = Map.of(
                    "orderId", orderId,
                    "newStatus", status,
                    "updatedBy", currentUser.getUsername(),
                    "notes", notes != null ? notes : ""
            );

            log.info("Order status updated by user: {} - Order: {} - New status: {}", currentUser.getUsername(), orderId, status);
            return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Order status updated successfully", data));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not found", request.getRequestURI()));
        } catch (Exception e) {
            log.error("Error updating order status for orderId: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to update order status: " + e.getMessage(), request.getRequestURI()));
        }
    }

    /**
     * POST /api/v1/manager/orders/{orderId}/assign-staff
     * Assign collection and analysis staff to an order
     */
    @PostMapping("/{orderId}/assign-staff")
    public ResponseEntity<ApiResponse<Map<String, Object>>> assignStaffToOrder(
            @PathVariable Long orderId,
            @RequestParam String collectStaffId,
            @RequestParam String analysisStaffId,
            @RequestParam(required = false) String assignmentType,
            @RequestParam(required = false) String notes,
            HttpServletRequest request) {

        try {
            // Authenticate user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Not authenticated", request.getRequestURI()));
            }
            User currentUser = userRepository.findByUsername(auth.getName())
                    .map(u -> userRepository.findById(u.getId())
                            .orElseThrow(() -> new RuntimeException("User not found")))
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));

            if (collectStaffId == null || collectStaffId.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Collection staff ID cannot be empty", request.getRequestURI()));
            }

            if (analysisStaffId == null || analysisStaffId.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Analysis staff ID cannot be empty", request.getRequestURI()));
            }

            StaffAvailableRequest collectStaffRequest = StaffAvailableRequest.builder()
                    .staffId(collectStaffId)
                    .build();
            StaffAvailableRequest analysisStaffRequest = StaffAvailableRequest.builder()
                    .staffId(analysisStaffId)
                    .build();

            orderTaskManagementService.taskAssignmentForStaff(orderId, collectStaffRequest, analysisStaffRequest);

            Map<String, Object> data = Map.of(
                    "orderId", orderId,
                    "collectStaffId", collectStaffId,
                    "analysisStaffId", analysisStaffId,
                    "assignmentType", assignmentType != null ? assignmentType : "STANDARD",
                    "assignedBy", currentUser.getUsername(),
                    "notes", notes != null ? notes : ""
            );

            log.info("Staff assigned to order by user: {} - Order: {} - Collection Staff: {}, Analysis Staff: {}",
                    currentUser.getUsername(), orderId, collectStaffId, analysisStaffId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(HttpStatus.CREATED.value(), "Staff assigned to order successfully", data));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not found", request.getRequestURI()));
        } catch (Exception e) {
            log.error("Error assigning staff to order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to assign staff: " + e.getMessage(), request.getRequestURI()));
        }
    }
}