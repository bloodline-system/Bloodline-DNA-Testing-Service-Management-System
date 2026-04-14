package com.dna_testing_system.dev.BVA;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.ManagerOrderController;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.dto.response.ServiceOrderResponse;
import com.dna_testing_system.dev.dto.response.StaffAvailableResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.ServiceOrderStatus;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ManagerOrderController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
})
@AutoConfigureMockMvc(addFilters = true)
class ManagerOrderBvaTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderTaskManagementService orderTaskManagementService;

    @MockitoBean
    UserRepository userRepository;

    @TestConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) {
            try {
                http.csrf(csrf -> csrf.disable())
                        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .exceptionHandling(ex -> ex
                                .authenticationEntryPoint((request, response, authException) -> response.setStatus(401))
                                .accessDeniedHandler((request, response, accessDeniedException) -> response.setStatus(403)));
                return http.build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ==================== PHAN 1: GET /api/v1/manager/orders ====================

    @Test
    void getAllOrders_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/manager/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void getAllOrders_roleStaff_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/manager/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getAllOrders_withOrders_returns200() throws Exception {
        User manager = User.builder().id("mgr-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("mgr-1")).thenReturn(Optional.of(manager));

        ServiceOrderResponse order = ServiceOrderResponse.builder()
                .id(1L)
                .customerName("John Doe")
                .orderStatus(ServiceOrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100))
                .createdAt(LocalDateTime.now())
                .build();
        when(orderTaskManagementService.getServiceOrders()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/manager/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(1));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getAllOrders_emptyOrders_returns200() throws Exception {
        User manager = User.builder().id("mgr-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("mgr-1")).thenReturn(Optional.of(manager));
        when(orderTaskManagementService.getServiceOrders()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/manager/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(0));
    }

    // ==================== PHAN 2: GET /api/v1/manager/orders/new ====================

    @Test
    void getNewOrders_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/manager/orders/new"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getNewOrders_withData_returns200() throws Exception {
        User manager = User.builder().id("mgr-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("mgr-1")).thenReturn(Optional.of(manager));

        ServiceOrderResponse newOrder = ServiceOrderResponse.builder()
                .id(2L)
                .customerName("Jane Smith")
                .orderStatus(ServiceOrderStatus.PENDING)
                .build();
        StaffAvailableResponse staff = StaffAvailableResponse.builder()
                .id("staff-1")
                .fullName("John Worker")
                .build();

        when(orderTaskManagementService.getNewOrders()).thenReturn(List.of(newOrder));
        when(orderTaskManagementService.getStaffAvailable()).thenReturn(List.of(staff));

        mockMvc.perform(get("/api/v1/manager/orders/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newOrdersCount").value(1))
                .andExpect(jsonPath("$.data.availableStaffCount").value(1));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getNewOrders_noNewOrders_returns200() throws Exception {
        User manager = User.builder().id("mgr-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("mgr-1")).thenReturn(Optional.of(manager));
        when(orderTaskManagementService.getNewOrders()).thenReturn(List.of());
        when(orderTaskManagementService.getStaffAvailable()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/manager/orders/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newOrdersCount").value(0))
                .andExpect(jsonPath("$.data.availableStaffCount").value(0));
    }

    // ==================== PHAN 3: PATCH /api/v1/manager/orders/{orderId}/status ====================

    @Test
    void updateOrderStatus_noToken_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/manager/orders/1/status")
                        .param("status", "COMPLETED"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updateOrderStatus_blankStatus_returns400() throws Exception {
        User manager = User.builder().id("mgr-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("mgr-1")).thenReturn(Optional.of(manager));

        mockMvc.perform(patch("/api/v1/manager/orders/1/status")
                        .param("status", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updateOrderStatus_validStatus_returns200() throws Exception {
        User manager = User.builder().id("mgr-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("mgr-1")).thenReturn(Optional.of(manager));
        doNothing().when(orderTaskManagementService).updateOrderStatus(1L, "COMPLETED");

        mockMvc.perform(patch("/api/v1/manager/orders/1/status")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk());
    }

    // ==================== PHAN 4: POST /api/v1/manager/orders/{orderId}/assign-staff ====================

    @Test
    void assignStaff_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/manager/orders/1/assign-staff")
                        .param("collectStaffId", "staff-1")
                        .param("analysisStaffId", "staff-2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void assignStaff_missingCollectStaff_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/manager/orders/1/assign-staff")
                        .param("analysisStaffId", "staff-2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void assignStaff_missingAnalysisStaff_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/manager/orders/1/assign-staff")
                        .param("collectStaffId", "staff-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void assignStaff_validRequest_returns201() throws Exception {
        User manager = User.builder().id("mgr-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("mgr-1")).thenReturn(Optional.of(manager));
        doNothing().when(orderTaskManagementService).taskAssignmentForStaff(eq(1L), any(), any());

        mockMvc.perform(post("/api/v1/manager/orders/1/assign-staff")
                        .param("collectStaffId", "staff-1")
                        .param("analysisStaffId", "staff-2"))
                .andExpect(status().isCreated());
    }
}
