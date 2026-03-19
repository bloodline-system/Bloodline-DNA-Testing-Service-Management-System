package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.OrderTestKitRequest;
import com.dna_testing_system.dev.dto.response.OrderTestKitResponse;
import com.dna_testing_system.dev.entity.Notification;
import com.dna_testing_system.dev.entity.OrderKit;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.TestKit;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.KitStatus;
import com.dna_testing_system.dev.enums.NotificationCategory;
import com.dna_testing_system.dev.enums.NotificationType;
import com.dna_testing_system.dev.enums.RoleType;
import com.dna_testing_system.dev.mapper.OrderTestKitMapper;
import com.dna_testing_system.dev.repository.OrderServiceRepository;
import com.dna_testing_system.dev.repository.OrderTestKitRepository;
import com.dna_testing_system.dev.repository.TestKitRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.NotificationService;
import com.dna_testing_system.dev.service.OrderKitService;
import com.dna_testing_system.dev.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderKitServiceImpl implements OrderKitService {
    OrderTestKitMapper orderTestKitMapper;
    OrderTestKitRepository orderKitRepository;
    TestKitRepository testKitRepository;
    OrderServiceRepository orderServiceRepository;
    UserRepository userRepository;
    NotificationService notificationService;

    @Override
    @Transactional
    public void createOrder(Long orderId, OrderTestKitRequest orderTestKitRequest) {
        ServiceOrder serviceOrder = orderServiceRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for ID: " + orderId));
        OrderKit orderKit = orderTestKitMapper.toOrderKit(orderTestKitRequest);
        Long idKit = orderTestKitRequest.getKitTestId();
        TestKit testKit = testKitRepository.findById(idKit)
                .orElseThrow(() -> new RuntimeException("Test Kit not found with id: " + idKit));;
        if (testKit == null) {
            throw new IllegalArgumentException("Test kit not found for ID: " + orderKit.getId());
        }
        orderKit.setUnitPrice(testKit.getCurrentPrice());
        orderKit.setTotalPrice(orderKit.getUnitPrice().multiply(BigDecimal.valueOf(orderKit.getQuantityOrdered().longValue())));
        // Save the orderKit to the database (repository save method would be called here)
        orderKit.setKit(testKit);
        KitStatus kitStatus = KitStatus.ORDERED;
        orderKit.setKitStatus(kitStatus);
        orderKit.setOrder(serviceOrder);
        orderKit.getOrder().getPayments().setGrossAmount(orderKit.getOrder().getPayments().getGrossAmount().add(orderKit.getTotalPrice()));
        orderKitRepository.save(orderKit);
        // Optionally, you might want to update the stock of the kit after ordering
        testKit.setQuantityInStock(
                testKit.getQuantityInStock() - orderKit.getQuantityOrdered());
        if( testKit.getQuantityInStock() <= 0) {
            testKit.setIsAvailable(false);
        }
        orderKit.setOrder(serviceOrder);
        testKitRepository.save(testKit);
        
        // Notify managers about the new order
        notifyToManagersForNewOrder(serviceOrder, orderKit);
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        orderKitRepository.deleteById(orderId);
    }

    @Override
    @Transactional
    public void updateOrder(Long orderId, OrderTestKitRequest orderTestKitRequest) {
        OrderKit orderKit = orderTestKitMapper.toOrderKit(orderTestKitRequest);
        TestKit testKit = testKitRepository.getOne(orderKit.getId());
        if (testKit == null) {
            throw new IllegalArgumentException("Test kit not found for ID: " + orderKit.getId());
        }
        if (orderTestKitRequest.getQuantityOrdered() < orderKit.getQuantityOrdered()) {
            // If the new quantity is less than the current quantity, we need to update the stock
            testKit.setQuantityInStock(testKit.getQuantityInStock() + (orderKit.getQuantityOrdered() - orderTestKitRequest.getQuantityOrdered()));

        } else {
            // If the new quantity is greater, we need to reduce the stock accordingly
            testKit.setQuantityInStock(testKit.getQuantityInStock() - (orderTestKitRequest.getQuantityOrdered() - orderKit.getQuantityOrdered()));
        }
        orderKit.setUnitPrice(testKit.getCurrentPrice());
        orderKit.setTotalPrice(orderKit.getUnitPrice().multiply(BigDecimal.valueOf(orderTestKitRequest.getQuantityOrdered().longValue())));
        // Update the orderKit in the database
        orderKitRepository.save(orderKit);
        // Update the test kit stock in the database
        testKitRepository.save(testKit);
    }

    @Override
    @Transactional
    public List<OrderTestKitResponse> getOrderById(Long orderId) {
        List<OrderTestKitResponse> orderTestKitResponses = new ArrayList<>();
        ServiceOrder serviceOrder = orderServiceRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for ID: " + orderId));
        serviceOrder.getOrderKits().forEach(orderKit -> {
                OrderTestKitResponse response = orderTestKitMapper.toOrderTestKitResponse(orderKit);
                TestKit testKit = orderKit.getKit();
                if (response != null) {
                    response.setKitName(testKit.getKitName());
                    response.setKitType(testKit.getKitType());
                    response.setSampleType(testKit.getSampleType());
                    orderTestKitResponses.add(response);
                }
        });
        return orderTestKitResponses;
    }

    @Override
    public List<OrderTestKitResponse> getAllOrders() {
        List<OrderKit> orderKits = orderKitRepository.findAll();
        return orderKits.stream()
                .map(orderTestKitMapper::toOrderTestKitResponse)
                .toList();
    }

    private void notifyToManagersForNewOrder(ServiceOrder order, OrderKit orderKit) {
        List<User> managers = userRepository.findUsersByRoleName(RoleType.MANAGER.name());

        for (User manager : managers) {
            Notification notification = Notification.builder()
                    .recipientUser(manager)
                    .subject("New Order")
                    .messageContent("A new order has just been created: " + order.getId() +
                            " with kit: " + orderKit.getKit().getKitName() +
                            " (Quantity: " + orderKit.getQuantityOrdered() + ")")
                    .notificationCategory(NotificationCategory.IN_APP)
                    .notificationType(NotificationType.ACCOUNT_ACTIVITY)
                    .build();

            notificationService.save(notification);
            notificationService.sendNotification(notification);
        }
    }
}
