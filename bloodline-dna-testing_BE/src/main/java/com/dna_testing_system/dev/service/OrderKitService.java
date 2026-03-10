package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.OrderTestKitRequest;
import com.dna_testing_system.dev.dto.response.OrderTestKitResponse;

import java.util.List;

public interface OrderKitService {
    void createOrder(Long orderId,OrderTestKitRequest orderTestKitRequest);
    void deleteOrder(Long orderId);
    void updateOrder(Long orderId, OrderTestKitRequest orderTestKitRequest);
    List<OrderTestKitResponse> getOrderById(Long orderId);
    List<OrderTestKitResponse> getAllOrders();
}
