package com.dna_testing_system.dev.service.staff;

import com.dna_testing_system.dev.dto.request.staff.PaymentUpdatingRequest;
import com.dna_testing_system.dev.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {
    void updatePaymentStatus(PaymentUpdatingRequest paymentUpdatingRequest);
    List<PaymentResponse> getPayments();
    Page<PaymentResponse> getPaymentsPage(String query, String status, Pageable pageable);
}
