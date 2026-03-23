package com.dna_testing_system.dev.dto.response.manager;

import com.dna_testing_system.dev.dto.response.ServiceOrderResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderManagementResponse {
    private List<ServiceOrderResponse> orders;
    private String pageTitle;
    private Map<String, Long> statusCounts;
    private Integer totalOrders;
}
