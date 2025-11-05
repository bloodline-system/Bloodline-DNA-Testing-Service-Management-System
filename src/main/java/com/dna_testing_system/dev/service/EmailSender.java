package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.entity.SampleCollection;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.TestResult;
import com.dna_testing_system.dev.entity.User;

public interface EmailSender {
    void sendNewOrderNotificationToManager(ServiceOrder order, String managerEmail);
    void sendTestAssignmentNotification(ServiceOrder order, SampleCollection sampleCollection, User staffMember);
    void sendTestAssignmentNotification(ServiceOrder order, TestResult testResult, User staffMember);
    void sendOrderStatusUpdateNotification(ServiceOrder order, String recipient);
}