package com.dna_testing_system.dev.controller.staff;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.staff.OrderStatusUpdateRequest;
import com.dna_testing_system.dev.dto.response.CRUDorderResponse;
import com.dna_testing_system.dev.dto.response.OrderParticipantResponse;
import com.dna_testing_system.dev.dto.response.OrderTestKitResponse;
import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.service.OrderKitService;
import com.dna_testing_system.dev.service.OrderParticipantService;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import com.dna_testing_system.dev.service.staff.StaffService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff/orders")
@SecurityRequirement(name = "bearerAuth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StaffOrderController {

    StaffService staffService;
    OrderTaskManagementService orderTaskManagementService;
    OrderKitService orderKitService;
    OrderParticipantService orderParticipantService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<CRUDorderResponse>> getOrders(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        String username = currentUsername();
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Orders retrieved successfully",
                PageResponse.from(staffService.getOrdersPage(username, status, pageable)));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CRUDorderResponse> getOrderById(@PathVariable Long id) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Order retrieved successfully",
                staffService.getOrderById(id));
    }

    @GetMapping("/{id}/kits")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<OrderTestKitResponse>> getOrderKits(@PathVariable Long id) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Order kits retrieved successfully",
                orderKitService.getOrderById(id));
    }

    @GetMapping("/{id}/participants")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<OrderParticipantResponse>> getOrderParticipants(@PathVariable Long id) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Order participants retrieved successfully",
                orderParticipantService.getAllParticipantsByOrderId(id));
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        orderTaskManagementService.updateOrderStatus(id, request.status());
        return ApiResponse.success(HttpStatus.OK.value(), "Order status updated successfully", null);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
