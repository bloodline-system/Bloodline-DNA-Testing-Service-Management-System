package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.ServiceOrderRequestByCustomer;
import com.dna_testing_system.dev.dto.response.ServiceOrderByCustomerResponse;
import com.dna_testing_system.dev.entity.MedicalService;
import com.dna_testing_system.dev.entity.Payment;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.PaymentMethod;
import com.dna_testing_system.dev.enums.PaymentStatus;
import com.dna_testing_system.dev.enums.ServiceOrderStatus;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.exception.ResourceNotFoundException;
import com.dna_testing_system.dev.mapper.OrderServiceMapper;
import com.dna_testing_system.dev.repository.MedicalServiceRepository;
import com.dna_testing_system.dev.repository.OrderServiceRepository;
import com.dna_testing_system.dev.repository.PaymentRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.OrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;



@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderServiceImpl implements OrderService {
    final OrderServiceRepository orderServiceRepository;
    final OrderServiceMapper orderServiceMapper;
    final UserRepository userRepository;
    final MedicalServiceRepository medicalServiceRepository;
    PaymentRepository paymentRepository;

    @Override
    @Transactional
    public ServiceOrderByCustomerResponse createOrder(ServiceOrderRequestByCustomer serviceOrderRequestByCustomer) {
        String username = serviceOrderRequestByCustomer.getUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_EXISTS));
        if (user== null) {
            throw new IllegalArgumentException("User not found with username: " + serviceOrderRequestByCustomer.getUsername());
        }
        MedicalService medicalService = medicalServiceRepository.findById(serviceOrderRequestByCustomer.getIdMedicalService())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MEDICAL_SERVICE_NOT_EXISTS));


        ServiceOrder serviceOrder = orderServiceMapper.toOrderService(serviceOrderRequestByCustomer);
        serviceOrder.setCustomer(user);
        serviceOrder.setService(medicalService);
        serviceOrder = orderServiceRepository.save(serviceOrder);

        Payment payment = Payment.builder()
                .paymentMethod(serviceOrderRequestByCustomer.getPaymentMethod())//pttt sau
                .paymentStatus(PaymentStatus.PENDING)
                .order(serviceOrder)
                .grossAmount(medicalService.getCurrentPrice())
                .discountAmount(BigDecimal.ZERO)
                .netAmount(medicalService.getCurrentPrice())
                .build();
        paymentRepository.save(payment);
        serviceOrder.setPayments(payment);

        serviceOrder = orderServiceRepository.save(serviceOrder);

        ServiceOrderByCustomerResponse response = orderServiceMapper.toServiceOrderByCustomerResponse(serviceOrder);
        if(response == null) {
            throw new IllegalArgumentException("Failed to create order service");
        }

        return response;
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        orderServiceRepository.deleteById(orderId);
    }

    @Override
    @Transactional
    public void updateOrder(Long orderId, ServiceOrderStatus serviceOrderStatus) {
        ServiceOrder serviceOrder = orderServiceRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for ID: " + orderId));
        if (serviceOrder == null) {
            throw new IllegalArgumentException("Order not found for ID: " + orderId);
        }
        serviceOrder.setOrderStatus(serviceOrderStatus);
        orderServiceRepository.save(serviceOrder);
    }

    @Override
    @Transactional
    public List<ServiceOrderByCustomerResponse> getAllOrdersByCustomerId(String customerName) {
        List<ServiceOrderByCustomerResponse> response = new ArrayList<>();
        List<ServiceOrder> serviceOrderList = orderServiceRepository.findAll();
        List<ServiceOrder> filteredOrders = serviceOrderList.stream()
                .filter(order -> order.getCustomer().getUsername().equals(customerName))
                .toList();
        for( ServiceOrder order : filteredOrders) {
            MedicalService medicalService = order.getService();
            ServiceOrderByCustomerResponse orderResponse = orderServiceMapper.toServiceOrderByCustomerResponse(order);
            orderResponse.setMedicalServiceName(medicalService.getServiceName());
            response.add(orderResponse);
        }
        return response;
    }

    @Override
    public ServiceOrderByCustomerResponse getOrderById(Long orderId) {
        ServiceOrder serviceOrder = orderServiceRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for ID: " + orderId));
        MedicalService medicalService = serviceOrder.getService();
        ServiceOrderByCustomerResponse response = orderServiceMapper.toServiceOrderByCustomerResponse(serviceOrder);
        if (response != null) {
            response.setMedicalServiceName(medicalService.getServiceName());
            return response;
        }
        else {
            throw new IllegalArgumentException("Failed to retrieve order by ID: " + orderId);
        }
    }

    @Override
    @Transactional
    public void acceptOrder(Long orderId) {
        ServiceOrder serviceOrder = orderServiceRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for ID: " + orderId));

        serviceOrder.setOrderStatus(ServiceOrderStatus.CONFIRMED);
        orderServiceRepository.save(serviceOrder);
    }

    @Override
    public ServiceOrder getOrderByIdEntity(Long orderId) {
        ServiceOrder serviceOrder = orderServiceRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found for ID: " + orderId));
        return serviceOrder;
    }


}
