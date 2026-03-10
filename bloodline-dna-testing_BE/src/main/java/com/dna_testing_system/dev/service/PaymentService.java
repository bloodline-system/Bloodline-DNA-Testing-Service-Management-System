package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.PaymentUpdatingRequest;
import com.dna_testing_system.dev.dto.response.PaymentResponse;
import com.dna_testing_system.dev.enums.PaymentStatus;

import java.util.List;

public interface PaymentService {
    void updatePaymentStatus(PaymentUpdatingRequest paymentUpdatingRequest);
    List<PaymentResponse> getPayments();
}
