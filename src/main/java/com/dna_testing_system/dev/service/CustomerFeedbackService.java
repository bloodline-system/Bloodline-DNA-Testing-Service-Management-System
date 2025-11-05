package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.CreateFeedbackRequest;
import com.dna_testing_system.dev.dto.request.RespondFeedbackRequest;
import com.dna_testing_system.dev.dto.request.UpdatingFeedbackRequest;
import com.dna_testing_system.dev.dto.response.CustomerFeedbackResponse;
import com.dna_testing_system.dev.entity.CustomerFeedback;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CustomerFeedbackService {
    CustomerFeedbackResponse createFeedback(CreateFeedbackRequest request);
    List<CustomerFeedbackResponse> getFeedbackByCustomer(String customerId);
    Page<CustomerFeedbackResponse> getAllFeedbacks(int page, int size, String search);
    CustomerFeedbackResponse getFeedbackById(Long id);
    CustomerFeedbackResponse respondToFeedback(Long feedbackId, RespondFeedbackRequest request);
    void editingCustomerFeedback(Long feedbackId, UpdatingFeedbackRequest request);
    void deleteFeedback(Long feedbackId, String userId);
}
