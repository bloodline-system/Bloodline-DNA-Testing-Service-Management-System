package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.StaffAvailableRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/manager")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManagerOrderController extends BaseController {
    OrderTaskManagementService orderTaskManagementService;


    @PostMapping("/order-management/update-status")
    public String updateOrderStatus(@RequestParam Long orderId,
                                    @RequestParam String status,
                                    @RequestParam(required = false) String notes,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderTaskManagementService.updateOrderStatus(orderId, status);
            redirectAttributes.addAttribute("success", "true");
            log.info("Order status updated successfully for order ID: {}", orderId);
        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage());
            redirectAttributes.addAttribute("error", "true");
        }
        return "redirect:/manager/order-management";
    }

    @GetMapping("/order-management")
    public String orderManagement(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {

            String currentPrincipalName = authentication.getName();
            UserProfileResponse userProfile = userProfileService.getUserProfile(currentPrincipalName);
            model.addAttribute("userProfile", userProfile);
        }
        try {
            var orders = orderTaskManagementService.getServiceOrders();
            model.addAttribute("orders", orders);
            model.addAttribute("pageTitle", "Order Management");

            // Calculate counts for each status
            Map<String, Long> statusCounts = orders.stream()
                    .collect(Collectors.groupingBy(
                            order -> order.getOrderStatus().name().toLowerCase().replace("_", "-"),
                            Collectors.counting()
                    ));

            model.addAttribute("statusCounts", statusCounts);
            model.addAttribute("totalOrders", orders.size());

        } catch (Exception e) {
            log.error("Error loading orders: {}", e.getMessage());
            model.addAttribute("orders", new ArrayList<>());
            model.addAttribute("statusCounts", new HashMap<>());
            model.addAttribute("totalOrders", 0);
        }

        return "manager/order-management";
    }
    @GetMapping("/new-orders")
    public String newOrders(Model model) {
        model.addAttribute("pageTitle", "New Orders Management");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {

            String currentPrincipalName = authentication.getName();
            UserProfileResponse userProfile = userProfileService.getUserProfile(currentPrincipalName);
            model.addAttribute("userProfile", userProfile);
        }
        // Load new orders (unassigned orders with PENDING status)
        var newOrders = orderTaskManagementService.getNewOrders();

        // Load available staff
        var availableStaff = orderTaskManagementService.getStaffAvailable();

        // Calculate statistics
        model.addAttribute("newOrders", newOrders);
        model.addAttribute("availableStaff", availableStaff);
        model.addAttribute("newOrdersCount", newOrders.size());
        model.addAttribute("availableStaffCount", availableStaff.size());
        model.addAttribute("assignedTodayCount", 0); // This could be calculated from database
        model.addAttribute("pendingAssignmentCount", newOrders.size());

        return "manager/new-order-management";
    }

    @PostMapping("/assign-staff")
    public String assignStaff(@RequestParam Long orderId,
                              @RequestParam String collectStaffId,
                              @RequestParam String analysisStaffId,
                              @RequestParam(required = false) String assignmentType,
                              @RequestParam(required = false) String notes,
                              RedirectAttributes redirectAttributes) {
        try {
            StaffAvailableRequest collectStaffRequest = StaffAvailableRequest.builder()
                    .staffId(collectStaffId)
                    .build();
            StaffAvailableRequest analysisStaffRequest = StaffAvailableRequest.builder()
                    .staffId(analysisStaffId)
                    .build();
            // You can add notes field to StaffAvailableRequest if needed

            orderTaskManagementService.taskAssignmentForStaff(orderId, collectStaffRequest, analysisStaffRequest);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Staff successfully assigned to order!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to assign staff: " + e.getMessage());
        }

        return "redirect:/manager/new-orders";
    }
}
