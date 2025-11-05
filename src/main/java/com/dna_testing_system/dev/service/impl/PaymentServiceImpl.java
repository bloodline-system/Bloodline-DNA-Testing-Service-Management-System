package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.PaymentUpdatingRequest;
import com.dna_testing_system.dev.dto.response.PaymentResponse;
import com.dna_testing_system.dev.entity.*;
import com.dna_testing_system.dev.enums.*;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.exception.PaymentException;
import com.dna_testing_system.dev.mapper.PaymentMapper;
import com.dna_testing_system.dev.repository.PaymentRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.NotificationService;
import com.dna_testing_system.dev.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentServiceImpl implements PaymentService {
    PaymentRepository paymentRepository;
    NotificationService notificationService;
    UserRepository userRepository;
    PaymentMapper paymentMapper;
    @Override
    @Transactional
    public void updatePaymentStatus(PaymentUpdatingRequest paymentUpdatingRequest) {
        var existPayment = paymentRepository.findById(paymentUpdatingRequest.getPaymentId())
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_NOT_EXISTS));
        PaymentStatus newPaymentStatus;
        try {
            String enumStatus = paymentUpdatingRequest.getPaymentStatus().toUpperCase().replace("-", "_");
            newPaymentStatus = PaymentStatus.valueOf(enumStatus);
        } catch (IllegalArgumentException e) {
            throw new PaymentException(ErrorCode.PAYMENT_NOT_EXISTS);
        }
        validateStatusTransition(newPaymentStatus, existPayment.getPaymentStatus());

        try {
            existPayment.setPaymentStatus(newPaymentStatus);
            existPayment.setPaymentDate(LocalDateTime.now());
            existPayment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(existPayment);
            log.info("Successfully updated payment {} status to {}", existPayment.getId(), newPaymentStatus);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new PaymentException(ErrorCode.PAYMENT_PERSIST_ERROR);
        }
        notifyToManagersForCash(existPayment.getOrder(), existPayment);
    }

    @Override
    public List<PaymentResponse> getPayments() {
        var payments =  paymentRepository.findAll();
        return payments.stream().map(paymentMapper::toPaymentResponse).collect(Collectors.toList());
    }

    private void validateStatusTransition(PaymentStatus newStatus, PaymentStatus oldStatus) {
        if (newStatus.equals(PaymentStatus.PENDING) &&  oldStatus == PaymentStatus.PAID) {
            throw new PaymentException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private void notifyToManagersForCash(ServiceOrder order, Payment payment) {
        List<User> managers = userRepository.findUsersByRoleName(RoleType.MANAGER.name());

        for (User manager : managers) {
            Notification notification = Notification.builder()
                    .recipientUser(manager)
                    .subject("Collected money for an order")
                    .messageContent("A new order has just been collected: " + order.getId() +
                            " with amount: " + payment.getNetAmount())
                    .notificationCategory(NotificationCategory.IN_APP)
                    .notificationType(NotificationType.ORDER_UPDATE)
                    .build();

            notificationService.save(notification);
            notificationService.sendNotification(notification);
        }
    }
}
