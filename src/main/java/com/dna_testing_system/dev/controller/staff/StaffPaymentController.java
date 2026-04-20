package com.dna_testing_system.dev.controller.staff;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.staff.PaymentStatusUpdateRequest;
import com.dna_testing_system.dev.dto.request.staff.PaymentUpdatingRequest;
import com.dna_testing_system.dev.dto.PageResponse;
import com.dna_testing_system.dev.dto.response.PaymentResponse;
import com.dna_testing_system.dev.service.staff.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff/payments")
@SecurityRequirement(name = "bearerAuth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StaffPaymentController {

    PaymentService paymentService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<PaymentResponse>> getPayments(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ApiResponse.success(
                HttpStatus.OK.value(),
                "Payments retrieved successfully",
                PageResponse.from(paymentService.getPaymentsPage(query, status, pageable)));
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody PaymentStatusUpdateRequest request) {
        PaymentUpdatingRequest updatingRequest = new PaymentUpdatingRequest();
        updatingRequest.setPaymentId(id);
        updatingRequest.setPaymentStatus(request.paymentStatus());
        paymentService.updatePaymentStatus(updatingRequest);
        return ApiResponse.success(HttpStatus.OK.value(), "Payment status updated successfully", null);
    }
}
