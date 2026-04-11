package com.dna_testing_system.dev.BVA;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.controller.api.ApiAdminDashboardRestController;
import com.dna_testing_system.dev.entity.Role;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserProfile;
import com.dna_testing_system.dev.entity.UserRole;
import com.dna_testing_system.dev.repository.RoleRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.repository.UserRoleRepository;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import com.dna_testing_system.dev.service.SystemReportService;
import com.dna_testing_system.dev.service.UserProfileService;
import com.dna_testing_system.dev.service.service.MedicalServiceManageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApiAdminDashboardRestController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
})
@AutoConfigureMockMvc(addFilters = false)
class StaffManagementBvaTest {

    @Autowired
    MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    MedicalServiceManageService medicalServiceManageService;
    @MockitoBean
    OrderTaskManagementService orderTaskManagementService;
    @MockitoBean
    SystemReportService systemReportService;
    @MockitoBean
    UserProfileService userProfileService;
    @MockitoBean
    UserRepository userRepository;
    @MockitoBean
    RoleRepository roleRepository;
    @MockitoBean
    UserRoleRepository userRoleRepository;

    private static User userWithProfileAndRole(String id, String username, String email, String roleName) {
        User u = User.builder()
                .id(id)
                .username(username)
                .isActive(true)
                .build();
        UserProfile p = UserProfile.builder().email(email).phoneNumber("0900000000").build();
        p.setUser(u);
        u.setProfile(p);

        Role role = Role.builder().id(1L).roleName(roleName).build();
        UserRole ur = UserRole.builder().id(1L).user(u).role(role).build();
        u.setUserRoles(Set.of(ur));
        return u;
    }

    // ======================= GET /users BVA =======================

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsersList_size0_returns400() throws Exception {
        when(userRepository.findAll()).thenReturn(java.util.List.of());
        when(userRepository.findUsersByRoleName(any())).thenReturn(java.util.List.of());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/dashboard/users")
                .param("page", "0")
                .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid pagination parameters"));
    }

    @ParameterizedTest
    @WithMockUser(roles = "ADMIN")
    @CsvSource({
            "-1,1,400",
            "0,1,200",
            "1,1,200",
            "0,20,200"
    })
    void getUsersList_pageAndSizeBoundary(int page, int size, int expectedStatus) throws Exception {
        when(userRepository.findAll()).thenReturn(java.util.List.of(
                userWithProfileAndRole("u1", "staff1", "staff1@ex.com", "STAFF"),
                userWithProfileAndRole("u2", "manager1", "manager1@ex.com", "MANAGER")));
        when(userRepository.findUsersByRoleName(any())).thenReturn(java.util.List.of());

        var action = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/dashboard/users")
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .param("search", ""))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedStatus));

        if (expectedStatus == 200) {
            action.andExpect(jsonPath("$.data.users", hasSize(2)));
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsersList_searchEmpty_returnsAll() throws Exception {
        when(userRepository.findAll()).thenReturn(java.util.List.of(
                userWithProfileAndRole("u1", "staff1", "staff1@ex.com", "STAFF"),
                userWithProfileAndRole("u2", "manager1", "manager1@ex.com", "MANAGER")));
        when(userRepository.findUsersByRoleName(any())).thenReturn(java.util.List.of());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/dashboard/users")
                .param("search", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.users", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsersList_searchMatchesUsername_returnsSubset() throws Exception {
        when(userRepository.findAll()).thenReturn(java.util.List.of(
                userWithProfileAndRole("u1", "staff1", "staff1@ex.com", "STAFF"),
                userWithProfileAndRole("u2", "manager1", "manager1@ex.com", "MANAGER")));
        when(userRepository.findUsersByRoleName(any())).thenReturn(java.util.List.of());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/dashboard/users")
                .param("search", "staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.users", hasSize(1)))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.users[0].username").value("staff1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsersList_searchVeryLong_returns200NoFailure() throws Exception {
        when(userRepository.findAll()).thenReturn(java.util.List.of(
                userWithProfileAndRole("u1", "staff1", "staff1@ex.com", "STAFF")));
        when(userRepository.findUsersByRoleName(any())).thenReturn(java.util.List.of());

        String longSearch = "s".repeat(1024);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/dashboard/users")
                .param("search", longSearch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_found_returns200() throws Exception {
        User user = userWithProfileAndRole("u1", "staff1", "staff1@ex.com", "STAFF");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/dashboard/users/{userId}", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("staff1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_notFound_returns404() throws Exception {
        when(userRepository.findById("u2")).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/dashboard/users/{userId}", "u2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ======================= PUT /users/{id} BVA =======================

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_roleValidUppercase_returns200() throws Exception {
        User existing = userWithProfileAndRole("u1", "staff1", "old@ex.com", "CUSTOMER");
        when(userRepository.findById("u1")).thenReturn(Optional.of(existing));
        when(roleRepository.findByRoleName("STAFF"))
                .thenReturn(Optional.of(Role.builder().id(1L).roleName("STAFF").build()));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admin/dashboard/users/{id}", "u1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(java.util.Map.of("role", "STAFF"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User updated successfully"));
    }

    @ParameterizedTest
    @WithMockUser(roles = "ADMIN")
    @CsvSource({
            "staff",
            "INVALID_ROLE",
            "''"
    })
    void updateUser_roleInvalid_returns500(String role) throws Exception {
        User existing = userWithProfileAndRole("u1", "staff1", "old@ex.com", "CUSTOMER");
        when(userRepository.findById("u1")).thenReturn(Optional.of(existing));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admin/dashboard/users/{id}", "u1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(java.util.Map.of("role", role))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Unable to update user"))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/dashboard/users/u1"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_activeWrongType_returns500() throws Exception {
        User existing = userWithProfileAndRole("u1", "staff1", "old@ex.com", "CUSTOMER");
        when(userRepository.findById("u1")).thenReturn(Optional.of(existing));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admin/dashboard/users/{id}", "u1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":\"true\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Unable to update user"));

        verify(userRepository, never()).save(any());
    }
}
