package com.dna_testing_system.dev.dto.response.manager;

import com.dna_testing_system.dev.dto.response.ServiceOrderResponse;
import com.dna_testing_system.dev.dto.response.StaffAvailableResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewOrdersResponse {
    private List<ServiceOrderResponse> newOrders;
    private List<StaffAvailableResponse> availableStaff;
    private Integer newOrdersCount;
    private Integer availableStaffCount;
    private Integer assignedTodayCount;
    private Integer pendingAssignmentCount;
}
