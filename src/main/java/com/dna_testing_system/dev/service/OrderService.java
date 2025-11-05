package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.ServiceOrderRequestByCustomer;
import com.dna_testing_system.dev.dto.response.ServiceOrderByCustomerResponse;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.enums.ServiceOrderStatus;

import java.util.List;

public interface OrderService {
    ServiceOrderByCustomerResponse createOrder(ServiceOrderRequestByCustomer serviceOrderRequestByCustomer);
    void cancelOrder(Long orderId);
    void updateOrder(Long orderId, ServiceOrderStatus serviceOrderStatus);
    List<ServiceOrderByCustomerResponse> getAllOrdersByCustomerId(String customerName);
    ServiceOrderByCustomerResponse getOrderById(Long orderId);
    void acceptOrder(Long orderId);
    ServiceOrder getOrderByIdEntity(Long orderId);
}
