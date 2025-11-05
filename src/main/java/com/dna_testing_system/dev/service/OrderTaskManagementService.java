package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.StaffAvailableRequest;
import com.dna_testing_system.dev.dto.response.ServiceOrderResponse;
import com.dna_testing_system.dev.dto.response.StaffAvailableResponse;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.ServiceOrderStatus;

import java.util.List;

public interface OrderTaskManagementService {
    List<StaffAvailableResponse> getStaffAvailable();
    void sampleCollectionTaskAssignment(ServiceOrder order, User staff);
    void testResultTaskAssignment(ServiceOrder order, User staff);
    void taskAssignmentForStaff(Long orderId, StaffAvailableRequest collectionStaff, StaffAvailableRequest analysisStaff);
    List<ServiceOrderResponse> getServiceOrders();
    List<ServiceOrderResponse> getNewOrders(); // Add method for new orders

    void updateOrderStatus(Long orderId, String status);

}
