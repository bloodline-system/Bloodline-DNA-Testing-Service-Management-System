package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.response.ServiceOrderResponse;
import com.dna_testing_system.dev.dto.response.StaffAvailableResponse;
import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.ManagerOrderController;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.ServiceOrderStatus;
import com.dna_testing_system.dev.repository.RoleRepository;
import com.dna_testing_system.dev.repository.UserProfileRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.repository.UserRoleRepository;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.NotificationService;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ManagerOrderController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
})
@AutoConfigureMockMvc(addFilters = true)
class ManagerOrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderTaskManagementService orderTaskManagementService;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    NotificationService notificationService;

    @MockitoBean
    RoleRepository roleRepository;

    @MockitoBean
    UserRoleRepository userRoleRepository;

    @MockitoBean
    UserProfileRepository userProfileRepository;

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) ->
                            response.setStatus(401)));
            return http.build();
        }
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getAllOrders_withManager_returns200() throws Exception {
        User manager = User.builder().id("user-1").username("manager").build();

        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(manager));

        ServiceOrderResponse order = ServiceOrderResponse.builder()
                .id(100L)
                .customerName("Test User")
                .orderStatus(ServiceOrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(120))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderTaskManagementService.getServiceOrders()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/manager/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orders[0].id").value(100))
                .andExpect(jsonPath("$.data.totalOrders").value(1))
                .andExpect(jsonPath("$.data.statusCounts.pending").value(1));

        verify(orderTaskManagementService).getServiceOrders();
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getNewOrders_withManager_returns200() throws Exception {
        User manager = User.builder().id("user-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(manager));

        ServiceOrderResponse newOrder = ServiceOrderResponse.builder().id(300L).orderStatus(ServiceOrderStatus.PENDING).build();
        StaffAvailableResponse staff = StaffAvailableResponse.builder().id("staff-1").fullName("Worker").build();

        when(orderTaskManagementService.getNewOrders()).thenReturn(List.of(newOrder));
        when(orderTaskManagementService.getStaffAvailable()).thenReturn(List.of(staff));

        mockMvc.perform(get("/api/v1/manager/orders/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newOrdersCount").value(1))
                .andExpect(jsonPath("$.data.availableStaffCount").value(1));

        verify(orderTaskManagementService).getNewOrders();
        verify(orderTaskManagementService).getStaffAvailable();
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void updateOrderStatus_withManager_returns200() throws Exception {
        User manager = User.builder().id("user-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(manager));

        doNothing().when(orderTaskManagementService).updateOrderStatus(123L, "COMPLETED");

        mockMvc.perform(patch("/api/v1/manager/orders/123/status").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(123))
                .andExpect(jsonPath("$.data.newStatus").value("COMPLETED"));

        verify(orderTaskManagementService).updateOrderStatus(123L, "COMPLETED");
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void assignStaffToOrder_withManager_returns201() throws Exception {
        User manager = User.builder().id("user-1").username("manager").build();
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(manager));

        doNothing().when(orderTaskManagementService).taskAssignmentForStaff(eq(321L), any(), any());

        mockMvc.perform(post("/api/v1/manager/orders/321/assign-staff")
                        .param("collectStaffId", "collect-1")
                        .param("analysisStaffId", "analysis-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderId").value(321))
                .andExpect(jsonPath("$.data.collectStaffId").value("collect-1"))
                .andExpect(jsonPath("$.data.analysisStaffId").value("analysis-1"));

        verify(orderTaskManagementService).taskAssignmentForStaff(eq(321L), any(), any());
    }

    @Test
    void getAllOrders_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/manager/orders")).andExpect(status().isUnauthorized());
    }
}
