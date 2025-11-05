package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.OrderTestKitRequest;
import com.dna_testing_system.dev.dto.request.ParticipantRequest;
import com.dna_testing_system.dev.dto.request.ServiceOrderRequestByCustomer;
import com.dna_testing_system.dev.dto.response.*;
import com.dna_testing_system.dev.entity.MedicalService;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.enums.CollectionType;
import com.dna_testing_system.dev.enums.ServiceOrderStatus;
import com.dna_testing_system.dev.service.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderingByCustomer {
    OrderService orderService;
    OrderKitService orderKitService;
    OrderParticipantService orderParticipantService;
    MedicalServiceManageService medicalService;
    TestKitService testKitService;
    UserProfileService userProfileService;
    CustomerFeedbackService customerFeedbackService;
    // Controller đã được sửa
    @GetMapping("/user/list-service")
    public String listMedicalServices(Model model) {
        // ---- BẮT ĐẦU SỬA LỖI ----
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            UserProfileResponse userProfile = userProfileService.getUserProfile(authentication.getName());
            model.addAttribute("userProfile", userProfile);
        }
        // ---- KẾT THÚC SỬA LỖI ----

        List<MedicalServiceResponse> medicalServiceResponse = medicalService.getAllServices();
        model.addAttribute("medicalServices", medicalServiceResponse);

        return "CustomerOrderService/list-service";
    }

    @GetMapping("/user/order-service")
    public String orderServiceGet(Model model, @RequestParam("medicalServiceId") Long medicalServiceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();

        UserProfileResponse userProfile = userProfileService.getUserProfile(currentPrincipalName);
        model.addAttribute("userProfile", userProfile);


        ServiceOrderRequestByCustomer serviceOrderRequestByCustomer = new ServiceOrderRequestByCustomer();
        serviceOrderRequestByCustomer.setUsername(currentPrincipalName);
        serviceOrderRequestByCustomer.setIdMedicalService(medicalServiceId);
        model.addAttribute("today", LocalDateTime.now());
        model.addAttribute("collectionTypes", CollectionType.values());
        model.addAttribute("serviceOrderRequestByCustomer", serviceOrderRequestByCustomer);

        return "CustomerOrderService/order-service";
    }

    @PostMapping("/user/order-service")
    public String orderServicePost(Model model, @ModelAttribute("serviceOrderRequestByCustomer") ServiceOrderRequestByCustomer serviceOrderRequestByCustomer) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();

        UserProfileResponse userProfile = userProfileService.getUserProfile(currentPrincipalName);
        model.addAttribute("userProfile", userProfile);

        ServiceOrderByCustomerResponse serviceOrderByCustomerResponse = orderService.createOrder(serviceOrderRequestByCustomer);
        model.addAttribute("serviceOrderByCustomerResponse", serviceOrderByCustomerResponse);
        model.addAttribute("paymentMethod", serviceOrderByCustomerResponse.getPayments().getPaymentMethod());
        return "CustomerOrderService/order-service";
    }

    @GetMapping("/user/participant-information")
    public String participantInformation(Model model, @RequestParam("orderId") Long orderId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserProfileResponse userProfile = userProfileService.getUserProfile(authentication.getName());
            model.addAttribute("userProfile", userProfile);
        }
        Integer count = 0;
        ServiceOrder serviceOrder = orderService.getOrderByIdEntity(orderId);
        MedicalServiceResponse medicalServiceResponse = medicalService.getServiceById(serviceOrder.getService().getId());
        List<OrderParticipantResponse> orderParticipants = orderParticipantService.getAllParticipantsByOrderId(orderId);
        if (orderParticipants != null && !orderParticipants.isEmpty()) {
            count = orderParticipants.size();
            model.addAttribute("count", count);
        } else {
            model.addAttribute("count", count);
        }
        model.addAttribute("today", LocalDateTime.now());
        model.addAttribute("orderId", orderId);
        model.addAttribute("participantRequest", new ParticipantRequest());
        model.addAttribute("medicalServiceResponse", medicalServiceResponse);
        return "ParticipantOrder/ParticipantInformation";
    }

    @PostMapping("/user/participant-information")
    public String participantInformationPost(Model model, @ModelAttribute("participantRequest") ParticipantRequest participantRequest, @RequestParam("orderId") Long orderId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserProfileResponse userProfile = userProfileService.getUserProfile(authentication.getName());
            model.addAttribute("userProfile", userProfile);
        }

        // Code cũ giữ nguyên
        Integer count = 0;
        orderParticipantService.createOrderParticipant(orderId, participantRequest);
        ServiceOrder serviceOrder = orderService.getOrderByIdEntity(orderId);
        MedicalServiceResponse medicalServiceResponse = medicalService.getServiceById(serviceOrder.getService().getId());
        List<OrderParticipantResponse> orderParticipants = orderParticipantService.getAllParticipantsByOrderId(orderId);
        if (orderParticipants != null && !orderParticipants.isEmpty()) {
            count = orderParticipants.size();
            model.addAttribute("count", count);
        } else {
            model.addAttribute("count", count);
        }
        model.addAttribute("message", "Participant information saved successfully!");
        model.addAttribute("orderId", orderId);
        model.addAttribute("medicalServiceResponse", medicalServiceResponse);
        return "ParticipantOrder/ParticipantInformation";
    }

    @GetMapping("/user/list-kit")
    public String listKits(Model model, @RequestParam("orderId") Long orderId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserProfileResponse userProfile = userProfileService.getUserProfile(authentication.getName());
            model.addAttribute("userProfile", userProfile);
        }

        List<TestKitResponse> testKitResponse = testKitService.GetTestKitResponseList();
        model.addAttribute("testKitResponse", testKitResponse);
        model.addAttribute("orderId", orderId);
        return "OrderKit/list-kit";
    }


    @GetMapping("/user/order-kit")
    public String orderKit(@RequestParam("kitTestId") Long kitTestId, Model model, @RequestParam("orderId") Long orderId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserProfileResponse userProfile = userProfileService.getUserProfile(authentication.getName());
            model.addAttribute("userProfile", userProfile);
        }

        OrderTestKitRequest request = new OrderTestKitRequest();
        TestKitResponse testKit = testKitService.GetTestKitResponseById(kitTestId);
        request.setKitTestId(kitTestId);
        request.setOrderId(orderId);
        model.addAttribute("orderTestKitRequest", request);
        model.addAttribute("maxQuantity", testKit.getQuantityInStock());
        return "OrderKit/order-kit";
    }



    @PostMapping("/user/order-kit")
    public String submitOrder(@ModelAttribute("orderTestKitRequest") OrderTestKitRequest orderTestKitRequest,
                              Model model, RedirectAttributes redirectAttributes) {


        orderKitService.createOrder(orderTestKitRequest.getOrderId(), orderTestKitRequest);


        redirectAttributes.addFlashAttribute("message", "Order placed successfully!");


        return "redirect:/user/detail?orderId=" + orderTestKitRequest.getOrderId();
    }


    @GetMapping("/user/detail")
    public String detail(Model model, @RequestParam("orderId") Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            UserProfileResponse userProfile = userProfileService.getUserProfile(authentication.getName());
            model.addAttribute("userProfile", userProfile);
        }
        ServiceOrderByCustomerResponse detail = orderService.getOrderById(orderId);
        List<OrderTestKitResponse> testKitDetails = orderKitService.getOrderById(orderId);
        List<OrderParticipantResponse> participantDetails = orderParticipantService.getAllParticipantsByOrderId(orderId);
        model.addAttribute("detail", detail);
        model.addAttribute("testKitDetails",testKitDetails);
        model.addAttribute("participantDetails", participantDetails);
        BigDecimal paymentTotal = BigDecimal.ZERO;
        for( OrderTestKitResponse kit : testKitDetails) {
            paymentTotal = paymentTotal.add(kit.getTotalPrice());
        }
        paymentTotal = paymentTotal.add(detail.getFinalAmount());
        model.addAttribute("paymentTotal",paymentTotal);
        return "CustomerOrderService/detail";
    }


//    @PostMapping("/user/cancel")
//    public String cancel(@RequestParam("orderId") Long orderId) {
//        orderService.cancelOrder(orderId);
//        return "redirect:/order-history";
//    }

    @GetMapping("/user/order-history")
    public String orderHistory(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            UserProfileResponse userProfile = userProfileService.getUserProfile(authentication.getName());
            model.addAttribute("userProfile", userProfile);


            List<ServiceOrderByCustomerResponse> orders = orderService.getAllOrdersByCustomerId(authentication.getName());
            model.addAttribute("orders", orders);

        }

        return "order-history";
    }

    @PostMapping("/user/cancel")
    public String cancelOrder(@RequestParam("orderId") Long orderId, RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(orderId);
            redirectAttributes.addFlashAttribute("message", "Order cancelled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to cancel order: " + e.getMessage());
        }

        return "redirect:/order-history";

    }

    @PostMapping("/user/accept")
    public String acceptOrder(@RequestParam("orderId") Long orderId, RedirectAttributes redirectAttributes) {
        try {

            orderService.acceptOrder(orderId);
            redirectAttributes.addFlashAttribute("message", "Order accepted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to accept order: " + e.getMessage());
        }

        return "redirect:/order-history";
    }

    @GetMapping("/user/order-details")
    public String orderDetails(Model model, @RequestParam("orderId") Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserProfileResponse userProfile = null;
        
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            userProfile = userProfileService.getUserProfile(authentication.getName());
            model.addAttribute("userProfile", userProfile);
        }

        // Lấy dữ liệu cho order-details.html
        ServiceOrderByCustomerResponse orderDetails = orderService.getOrderById(orderId);
        List<OrderTestKitResponse> orderTestKits = orderKitService.getOrderById(orderId);
        List<OrderParticipantResponse> orderParticipants = orderParticipantService.getAllParticipantsByOrderId(orderId);

        model.addAttribute("orderDetails", orderDetails);
        model.addAttribute("orderTestKits", orderTestKits);
        model.addAttribute("orderParticipants", orderParticipants);

        BigDecimal paymentTotal = BigDecimal.ZERO;
        for( OrderTestKitResponse kit : orderTestKits) {
            paymentTotal = paymentTotal.add(kit.getTotalPrice());
        }
        paymentTotal = paymentTotal.add(orderDetails.getFinalAmount());
        model.addAttribute("paymentTotal",paymentTotal);

        // Check for existing feedback if order is completed and user is authenticated
        if ("COMPLETED".equals(orderDetails.getOrderStatus().name()) && userProfile != null) {
            try {
                String currentUserId = userProfile.getUserId();
                log.info("Looking for feedback for order {} and user {}", orderId, currentUserId);
                
                List<CustomerFeedbackResponse> userFeedbacks = customerFeedbackService.getFeedbackByCustomer(currentUserId);
                log.info("Found {} total feedbacks for user {}", userFeedbacks.size(), currentUserId);
                
                // Find feedback for this specific order
                CustomerFeedbackResponse existingFeedback = userFeedbacks.stream()
                    .filter(feedback -> feedback != null && orderId.equals(feedback.getOrderId()))
                    .findFirst()
                    .orElse(null);
                
                if (existingFeedback != null) {
                    log.info("Found existing feedback for order {}: title={}, hasResponse={}", 
                        orderId, existingFeedback.getFeedbackTitle(), 
                        existingFeedback.getResponseContent() != null);
                } else {
                    log.info("No existing feedback found for order {}", orderId);
                }
                
                model.addAttribute("existingFeedback", existingFeedback);
            } catch (Exception e) {
                log.error("Error checking for existing feedback for order: {}", orderId, e);
                // Continue without feedback data
                model.addAttribute("existingFeedback", null);
            }
        } else {
            log.info("Order {} status: {}, User authenticated: {}", orderId, orderDetails.getOrderStatus(), userProfile != null);
        }

        return "order-details"; // trả về template order-details.html
    }
}
