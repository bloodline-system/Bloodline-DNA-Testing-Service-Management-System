package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.StaffAvailableRequest;
import com.dna_testing_system.dev.dto.response.ServiceOrderResponse;
import com.dna_testing_system.dev.dto.response.StaffAvailableResponse;
import com.dna_testing_system.dev.entity.*;
import com.dna_testing_system.dev.enums.*;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.exception.ManagerException;
import com.dna_testing_system.dev.exception.ResourceNotFoundException;
import com.dna_testing_system.dev.mapper.ServiceOrderMapper;
import com.dna_testing_system.dev.repository.*;
import com.dna_testing_system.dev.service.EmailSender;
import com.dna_testing_system.dev.service.NotificationService;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderTaskManagementServiceImpl implements OrderTaskManagementService {
    UserRepository userRepository;
    SampleCollectionRepository sampleCollectionRepository;
    TestResultRepository testResultRepository;
    OrderServiceRepository orderServiceRepository;
    EmailSender emailSender;
    ServiceOrderMapper serviceOrderMapper;
    NotificationService notificationService;

    @Override
    public List<StaffAvailableResponse> getStaffAvailable() {
        List<User> staffUser = userRepository.findAll().stream()
                .filter(user -> user.getUserRoles()
                        .stream().anyMatch(userRole -> userRole.getRole().getRoleName().equals(RoleType.STAFF.name())))
                .toList();

        var staffsExistInCollectionSample = sampleCollectionRepository.findAll()
                .stream().filter(x -> x.getCollectionStatus().equals(CollectionStatus.PENDING) || x.getCollectionStatus().equals(CollectionStatus.SCHEDULED))
                .map(samp -> samp.getStaff().getId())
                .toList(); // staff exits in sample collection is inAvailable
        var staffsExistInTestResult = testResultRepository.findAll()
                .stream()
                .filter(x -> !x.getResultStatus().equals(ResultStatus.COMPLETED) && !x.getResultStatus().equals(ResultStatus.CANCELLED))
                .map(testResult -> testResult.getAnalyzedByStaff().getId())
                .toList(); // same

        List<User> staffAvailable = staffUser.stream()
                .filter(staff -> !staffsExistInCollectionSample.contains(staff.getId())
                && !staffsExistInTestResult.contains(staff.getId()))
                .toList();
        if  (staffAvailable.isEmpty()) {
            return new ArrayList<>();
        }
        List<StaffAvailableResponse> staffAvailableResponses = new ArrayList<>();
        for (User user : staffAvailable) {
            StaffAvailableResponse staffAvailableResponse = StaffAvailableResponse.builder()
                    .id(user.getId())
                    .email(user.getUserProfile().getEmail())
                    .username(user.getUsername())
                    .profileImageUrl(user.getUserProfile().getProfileImageUrl())
                    .roleName(user.getUserRoles().stream().map(x -> x.getRole().getRoleName()).collect(Collectors.toSet()))
                    .isActive(user.getIsActive())
                    .createdAt(user.getUserProfile().getCreatedAt())
                    .fullName(user.getUserProfile().getFirstName() + " " + user.getUserProfile().getLastName())
                    .phoneNumber(user.getUserProfile().getPhoneNumber())
                    .build();
            staffAvailableResponses.add(staffAvailableResponse);
        }
        return staffAvailableResponses;
    }

    @Override
    @Transactional
    public void sampleCollectionTaskAssignment(ServiceOrder order, User staffRequest) {
        log.info("Assigning sample collection task for order ID: {} to staff ID: {}", order.getId(), staffRequest.getId());


        SampleCollection sampleCollection = SampleCollection.builder()
                .order(order)
                .staff(staffRequest)
                .assignAt(LocalDateTime.now())
                .collectionDate(LocalDateTime.now())
                .collectionStatus(CollectionStatus.PENDING)
                .sampleType(SampleType.BLOOD)
                .sampleQuality(SampleQuality.EXCELLENT)
                .sampleType(SampleType.BLOOD)
                .build(); // mock, sửa sau

        try {
            emailSender.sendTestAssignmentNotification(order, sampleCollection, staffRequest);
            notifyToStaffForAssign(order, staffRequest);
            log.info("Email notification sent to staff member: {}", staffRequest.getUserProfile().getEmail());
        } catch (Exception e) {
            log.error("Failed to send email notification to staff: {}", e.getMessage());
            // Continue execution - email failure shouldn't prevent assignment
        }
        sampleCollectionRepository.save(sampleCollection);
    }

    @Override
    @Transactional
    public void testResultTaskAssignment(ServiceOrder order, User staffRequest) {
        log.info("Assigning testing and updating result new task for order ID: {} to staff ID: {}", order.getId(), staffRequest.getId());

        TestResult testResult = TestResult.builder()
                .createdAt(LocalDateTime.now())
                .analyzedByStaff(staffRequest)
                .order(order)
                .resultStatus(ResultStatus.PENDING)
                .build(); // mock

        try {
            emailSender.sendTestAssignmentNotification(order, testResult, staffRequest);
            notifyToStaffForAssign(order, staffRequest);
            log.info("Email notification sent to staff member: {}", staffRequest.getUserProfile().getEmail());
        } catch (Exception e) {
            log.error("Failed to send email notification to staff: {}", e.getMessage());
            // Continue execution - email failure shouldn't prevent assignment
        }
        testResultRepository.save(testResult);

    }

    @Override
    @Transactional
    public void taskAssignmentForStaff(Long orderId, StaffAvailableRequest collectionStaff, StaffAvailableRequest analysisStaff) {
        if (collectionStaff.getStaffId().equals(analysisStaff.getStaffId()))
            throw new ManagerException(ErrorCode.INVALID_STAFF_ASSIGNMENT);
        var order = orderServiceRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND));
        var collectStaff = userRepository.findById(collectionStaff.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND));
        var analysisByStaff = userRepository.findById(analysisStaff.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND));

        try {
            sampleCollectionTaskAssignment(order, collectStaff);
            testResultTaskAssignment(order, analysisByStaff);

            order.setOrderStatus(ServiceOrderStatus.CONFIRMED);
            orderServiceRepository.save(order);
            log.info("Successfully completed dual task assignment for order ID: {}", orderId);

        } catch (Exception e) {
            log.error("Failed dual task assignment for order ID: {} - {}", orderId, e.getMessage());
            throw new ManagerException(ErrorCode.ASSIGNMENT_FAILED);
        }
    }

    @Override
    public List<ServiceOrderResponse> getServiceOrders() {
        var orders = orderServiceRepository.findAll();
        List<ServiceOrderResponse> serviceOrderResponses = new ArrayList<>();
        for (ServiceOrder serviceOrder : orders) {
            ServiceOrderResponse serviceOrderResponse = serviceOrderMapper.toDto(serviceOrder);
            serviceOrderResponses.add(serviceOrderResponse);
        }
        return serviceOrderResponses;
    }
    
    @Override
    public List<ServiceOrderResponse> getNewOrders() {

        var newOrders = orderServiceRepository.findAll().stream()
                .filter(order -> order.getOrderStatus() == ServiceOrderStatus.PENDING)
                .filter(order -> order.getSampleCollections().isEmpty()) // No sample collection assigned yet
                .toList();
        
        List<ServiceOrderResponse> serviceOrderResponses = new ArrayList<>();
        for (ServiceOrder serviceOrder : newOrders) {
            ServiceOrderResponse serviceOrderResponse = serviceOrderMapper.toDto(serviceOrder);
            serviceOrderResponses.add(serviceOrderResponse);
        }
        return serviceOrderResponses;
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        log.info("Updating order status for order ID: {} to status: {}", orderId, status);

        var order = orderServiceRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND));

        ServiceOrderStatus newStatus;
        try {
            String enumStatus = status.toUpperCase().replace("-", "_");
            newStatus = ServiceOrderStatus.valueOf(enumStatus);
        } catch (IllegalArgumentException e) {
            throw new ManagerException(ErrorCode.INVALID_STATUS_TRANSITION);
        }


        validateStatusTransition(order.getOrderStatus(), newStatus);

        order.setOrderStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        orderServiceRepository.save(order);
        log.info("Successfully updated order {} status to {}", orderId, newStatus);

        if (newStatus.equals(ServiceOrderStatus.CANCELLED)) {
            order.getSampleCollections().forEach(sampleCollection -> {
                sampleCollection.setCollectionStatus(CollectionStatus.CANCELLED);
            });
            order.getTestResults().forEach(testResult -> {
                testResult.setResultStatus(ResultStatus.CANCELLED);
            });
            order.getPayments().setPaymentStatus(PaymentStatus.FAILED);
        }
        if (newStatus.equals(ServiceOrderStatus.COMPLETED)) {
            order.getSampleCollections().forEach(sampleCollection -> {
                sampleCollection.setCollectionStatus(CollectionStatus.COLLECTED);
            });
            order.getTestResults().forEach(testResult -> {
                testResult.setResultStatus(ResultStatus.COMPLETED);
            });
        }

        if (newStatus.equals(ServiceOrderStatus.COMPLETED) || newStatus.equals(ServiceOrderStatus.CANCELLED)) {
            try {
                String userEmail = order.getCustomer().getUserProfile().getEmail();
                emailSender.sendOrderStatusUpdateNotification(order, userEmail);

                log.info("Status update email notification sent to user: {}", userEmail);
            } catch (Exception e) {
                log.error("Failed to send status update email notification: {}", e.getMessage());
            }
        }
        notifyToCustomerTrackingOrder(order);
    }

    private void validateStatusTransition(ServiceOrderStatus currentStatus, ServiceOrderStatus newStatus) {
        if (currentStatus == ServiceOrderStatus.COMPLETED && newStatus == ServiceOrderStatus.PENDING) {
            throw new ManagerException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        if (currentStatus == ServiceOrderStatus.CANCELLED && newStatus != ServiceOrderStatus.CANCELLED) {
            throw new ManagerException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }
    private void notifyToCustomerTrackingOrder(ServiceOrder order) {
        Notification notification = Notification.builder()
                .recipientUser(order.getCustomer())
                .subject("Your order: " +  order.getId() + " has been updated")
                .messageContent("Your order has been " + order.getOrderStatus().toString())
                .notificationCategory(NotificationCategory.IN_APP)
                .notificationType(NotificationType.ORDER_UPDATE)
                .build();

        notificationService.save(notification);
        notificationService.sendNotification(notification);
        log.info("Successfully updated order {} status to {}", order.getId(), order.getOrderStatus());
    }

    private void notifyToStaffForAssign(ServiceOrder order, User recipient) {
        Notification notification = Notification.builder()
                .recipientUser(recipient)
                .subject("You have new task")
                .messageContent("You have new task with order " + order.getId())
                .notificationCategory(NotificationCategory.IN_APP)
                .notificationType(NotificationType.SYSTEM_ANNOUNCEMENT)
                .build();

        notificationService.save(notification);
        notificationService.sendNotification(notification);
        log.info("Successfully has order {} task to {}", order.getId(), recipient.getUsername());
    }
}
