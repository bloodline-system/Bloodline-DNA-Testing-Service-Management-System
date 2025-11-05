package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.CreateFeedbackRequest;
import com.dna_testing_system.dev.dto.request.RespondFeedbackRequest;
import com.dna_testing_system.dev.dto.request.UpdatingFeedbackRequest;
import com.dna_testing_system.dev.dto.response.CustomerFeedbackResponse;
import com.dna_testing_system.dev.entity.MedicalService;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.RoleType;
import com.dna_testing_system.dev.exception.EntityNotFoundException;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.exception.FeedbackException;
import com.dna_testing_system.dev.mapper.CustomerFeedbackMapper;
import com.dna_testing_system.dev.repository.CustomerFeedbackRepository;
import com.dna_testing_system.dev.repository.MedicalServiceRepository;
import com.dna_testing_system.dev.repository.OrderServiceRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.CustomerFeedbackService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerFeedbackServiceImpl implements CustomerFeedbackService {
    UserRepository userRepository;
    MedicalServiceRepository medicalServiceRepository;
    OrderServiceRepository orderServiceRepository;
    CustomerFeedbackRepository customerFeedbackRepository;
    CustomerFeedbackMapper customerFeedbackMapper;

    @Override
    public CustomerFeedbackResponse createFeedback(CreateFeedbackRequest request) {
        User userRequest = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));
        MedicalService medicalServiceRequest = medicalServiceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.MEDICAL_SERVICE_NOT_EXISTS));
        ServiceOrder orderRequest = null;
        if (request.getOrderId() != null) {
            orderRequest = orderServiceRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ORDER_NOT_EXISTS));
        }

        try {
            float avgRating = (
                    request.getServiceQualityRating() +
                            request.getStaffBehaviorRating() +
                            request.getTimelinessRating()
            ) / 3.0f;
            var newFeedback = customerFeedbackMapper.toEntity(request, userRequest, medicalServiceRequest, orderRequest);
            newFeedback.setOverallRating(avgRating);
            customerFeedbackRepository.save(newFeedback);
            return customerFeedbackMapper.toResponse(newFeedback);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new FeedbackException(ErrorCode.FEEDBACK_PERSIST_ERROR);
        }

    }

    @Override
    public List<CustomerFeedbackResponse> getFeedbackByCustomer(String customerId) {
        try {
            var customerFeedback = customerFeedbackRepository.findByCustomer_Id(customerId);
            return customerFeedback.stream().map(customerFeedbackMapper::toResponse).collect(Collectors.toList());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new FeedbackException(ErrorCode.FEEDBACK_GETTING_ERROR);
        }
    }

    @Override
    public Page<CustomerFeedbackResponse> getAllFeedbacks(int page, int size, String search) {
        return customerFeedbackRepository.findAllByFeedbackTitleContainsIgnoreCase(search, PageRequest.of(page, size))
                .map(customerFeedbackMapper::toResponse);
    }

    @Override
    public CustomerFeedbackResponse getFeedbackById(Long id) {
        var customerFeedback = customerFeedbackRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.FEEDBACK_GETTING_ERROR));
        return customerFeedbackMapper.toResponse(customerFeedback);
    }

    @Override
    public CustomerFeedbackResponse respondToFeedback(Long feedbackId, RespondFeedbackRequest request) {
        var feedbackNeedToRespond = customerFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.FEEDBACK_GETTING_ERROR));
        var userRespondFeedback = userRepository.findById(request.getRespondByUserId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));
        if (!feedbackNeedToRespond.getResponseRequired())
            throw new FeedbackException(ErrorCode.FEEDBACK_RESPONDING_ERROR);
        try {
            feedbackNeedToRespond.setRespondedAt(LocalDateTime.now());
            feedbackNeedToRespond.setRespondedBy(userRespondFeedback);
            feedbackNeedToRespond.setResponseContent(request.getResponseContent());
            customerFeedbackRepository.save(feedbackNeedToRespond);
            return customerFeedbackMapper.toResponse(feedbackNeedToRespond);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new FeedbackException(ErrorCode.FEEDBACK_RESPONDING_ERROR);
        }

    }

    @Override
    public void editingCustomerFeedback(Long feedbackId, UpdatingFeedbackRequest request) {
        var feedbackNeedToEdit = customerFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.FEEDBACK_GETTING_ERROR));
        try {
            float avgRating = (
                    request.getServiceQualityRating() +
                            request.getStaffBehaviorRating() +
                            request.getTimelinessRating()
            ) / 3.0f;
            customerFeedbackMapper.updateEntity(feedbackNeedToEdit, request);
            feedbackNeedToEdit.setOverallRating(avgRating);
            customerFeedbackRepository.save(feedbackNeedToEdit);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new FeedbackException(ErrorCode.FEEDBACK_PERSIST_ERROR);
        }
    }

    @Override
    public void deleteFeedback(Long feedbackId, String userId) {
        var userDeleteFeedback = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));
        var existingFeedback = customerFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.FEEDBACK_GETTING_ERROR));
        boolean isAdmin = userDeleteFeedback.getUserRoles()
                .stream().anyMatch(x -> x.getRole().getRoleName().equals(RoleType.ADMIN.name()));
        if (!isAdmin && !existingFeedback.getCustomer().getId().equals(userId)) {
            throw new FeedbackException(ErrorCode.UNAUTHORIZED_FEEDBACK_ACTION);
        }
        try {
            customerFeedbackRepository.deleteById(feedbackId);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new FeedbackException(ErrorCode.FEEDBACK_PERSIST_ERROR);
        }
    }
}
